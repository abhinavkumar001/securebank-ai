package com.securebank.fraud;

import com.securebank.account.AccountService;
import com.securebank.account.AccountType;
import com.securebank.account.dto.OpenAccountRequest;
import com.securebank.auth.AuthService;
import com.securebank.auth.Role;
import com.securebank.auth.dto.AuthResponse;
import com.securebank.auth.dto.RegisterRequest;
import com.securebank.common.ApiException;
import com.securebank.common.CurrentUser;
import com.securebank.transaction.TransactionService;
import com.securebank.transaction.dto.AmountRequest;
import com.securebank.transaction.dto.TransactionResponse;
import com.securebank.transaction.dto.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Held transfer -> admin approves or rejects -> balances are correct. */
@SpringBootTest
class ReviewFlowTest {

    @Autowired private AuthService authService;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;
    @Autowired private FraudAlertRepository alerts;

    private final CurrentUser admin = new CurrentUser(1L, "admin@securebank.com", Role.ADMIN);
    private CurrentUser alice;
    private String aliceAccount;
    private String bobAccount;

    @BeforeEach
    void setUp() {
        alice = customer("Alice");
        CurrentUser bob = customer("Bob");
        aliceAccount = accountService.open(alice, new OpenAccountRequest(AccountType.SAVINGS)).accountNumber();
        bobAccount = accountService.open(bob, new OpenAccountRequest(AccountType.SAVINGS)).accountNumber();
        transactionService.deposit(alice, new AmountRequest(aliceAccount, new BigDecimal("500000.00"), null), key());
    }

    @Test
    void approvingAHeldTransferMovesTheMoney() {
        Long alertId = holdTransfer("250000.00");

        TransactionResponse result = transactionService.approveHeld(admin, alertId, "Verified with customer by phone");

        assertEquals("SUCCESS", result.status());
        assertEquals(0, new BigDecimal("250000.00").compareTo(transactionService.balanceOf(aliceAccount)));
        assertEquals(0, new BigDecimal("250000.00").compareTo(transactionService.balanceOf(bobAccount)));
        assertEquals(FraudAlertStatus.APPROVED, alerts.findById(alertId).orElseThrow().getStatus());
    }

    @Test
    void rejectingAHeldTransferKeepsTheMoney() {
        Long alertId = holdTransfer("300000.00");

        TransactionResponse result = transactionService.rejectHeld(admin, alertId, "Customer did not confirm");

        assertEquals("REJECTED", result.status());
        assertEquals(0, new BigDecimal("500000.00").compareTo(transactionService.balanceOf(aliceAccount)));
        assertEquals(0, BigDecimal.ZERO.compareTo(transactionService.balanceOf(bobAccount)));
    }

    @Test
    void cannotReviewTheSameItemTwice() {
        Long alertId = holdTransfer("250000.00");
        transactionService.rejectHeld(admin, alertId, null);

        ApiException ex = assertThrows(ApiException.class, () -> transactionService.approveHeld(admin, alertId, null));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void approvalFailsIfTheBalanceIsNoLongerThere() {
        Long alertId = holdTransfer("400000.00");
        transactionService.withdraw(alice, new AmountRequest(aliceAccount, new BigDecimal("300000.00"), null), key());

        assertThrows(ApiException.class, () -> transactionService.approveHeld(admin, alertId, null));
        assertEquals(0, new BigDecimal("200000.00").compareTo(transactionService.balanceOf(aliceAccount)));
    }

    private Long holdTransfer(String amount) {
        TransactionResponse held = transactionService.transfer(alice,
                new TransferRequest(aliceAccount, bobAccount, new BigDecimal(amount), "Property advance"), key());
        assertEquals("HELD", held.status());
        return alerts.findByTransactionReference(held.reference()).orElseThrow().getId();
    }

    private CurrentUser customer(String name) {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@test.com";
        AuthResponse auth = authService.register(new RegisterRequest(name, email, "Passw0rd123"));
        return new CurrentUser(auth.userId(), email, Role.CUSTOMER);
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
