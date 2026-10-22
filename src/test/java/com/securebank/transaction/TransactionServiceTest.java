package com.securebank.transaction;

import com.securebank.account.AccountService;
import com.securebank.account.AccountType;
import com.securebank.account.dto.OpenAccountRequest;
import com.securebank.auth.AuthService;
import com.securebank.auth.Role;
import com.securebank.auth.dto.AuthResponse;
import com.securebank.auth.dto.RegisterRequest;
import com.securebank.common.ApiException;
import com.securebank.common.CurrentUser;
import com.securebank.transaction.dto.AmountRequest;
import com.securebank.transaction.dto.TransactionResponse;
import com.securebank.transaction.dto.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the real service against an H2 database.
 * Deliberately NOT @Transactional, so each service call commits on its own,
 * exactly like in production. That's what makes the concurrency test meaningful.
 */
@SpringBootTest
class TransactionServiceTest {

    @Autowired private AuthService authService;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;

    private CurrentUser alice;
    private CurrentUser bob;
    private String aliceAccount;
    private String bobAccount;

    @BeforeEach
    void setUp() {
        alice = newCustomer("Alice");
        bob = newCustomer("Bob");
        aliceAccount = accountService.open(alice, new OpenAccountRequest(AccountType.SAVINGS)).accountNumber();
        bobAccount = accountService.open(bob, new OpenAccountRequest(AccountType.SAVINGS)).accountNumber();
    }

    @Test
    void depositIncreasesBalance() {
        deposit(alice, aliceAccount, "1000.00");

        assertMoney("1000.00", transactionService.balanceOf(aliceAccount));
    }

    @Test
    void withdrawMoreThanBalanceFailsAndChangesNothing() {
        deposit(alice, aliceAccount, "100.00");

        ApiException ex = assertThrows(ApiException.class, () -> transactionService.withdraw(
                alice, new AmountRequest(aliceAccount, new BigDecimal("150.00"), null), key()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertMoney("100.00", transactionService.balanceOf(aliceAccount));
    }

    @Test
    void transferMovesMoneyBetweenAccounts() {
        deposit(alice, aliceAccount, "1000.00");

        TransactionResponse tx = transactionService.transfer(alice,
                new TransferRequest(aliceAccount, bobAccount, new BigDecimal("250.50"), "Rent"), key());

        assertEquals("SUCCESS", tx.status());
        assertMoney("749.50", transactionService.balanceOf(aliceAccount));
        assertMoney("250.50", transactionService.balanceOf(bobAccount));
    }

    @Test
    void retryWithSameIdempotencyKeyMovesMoneyOnlyOnce() {
        deposit(alice, aliceAccount, "1000.00");
        String key = key();
        TransferRequest request = new TransferRequest(aliceAccount, bobAccount, new BigDecimal("100.00"), null);

        TransactionResponse first = transactionService.transfer(alice, request, key);
        TransactionResponse retry = transactionService.transfer(alice, request, key);

        assertEquals(first.reference(), retry.reference());
        assertMoney("900.00", transactionService.balanceOf(aliceAccount));
        assertMoney("100.00", transactionService.balanceOf(bobAccount));
    }

    @Test
    void cannotTransferFromSomeoneElsesAccount() {
        deposit(bob, bobAccount, "500.00");

        ApiException ex = assertThrows(ApiException.class, () -> transactionService.transfer(alice,
                new TransferRequest(bobAccount, aliceAccount, new BigDecimal("100.00"), null), key()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertMoney("500.00", transactionService.balanceOf(bobAccount));
    }

    @Test
    void cannotTransferToSameAccount() {
        deposit(alice, aliceAccount, "500.00");

        assertThrows(ApiException.class, () -> transactionService.transfer(alice,
                new TransferRequest(aliceAccount, aliceAccount, new BigDecimal("10.00"), null), key()));
    }

    @Test
    void missingIdempotencyKeyIsRejected() {
        assertThrows(ApiException.class, () -> transactionService.deposit(
                alice, new AmountRequest(aliceAccount, new BigDecimal("10.00"), null), null));
    }

    @Test
    void highValueTransferIsHeldAndMoneyDoesNotMove() {
        deposit(alice, aliceAccount, "300000.00");

        TransactionResponse tx = transactionService.transfer(alice,
                new TransferRequest(aliceAccount, bobAccount, new BigDecimal("250000.00"), null), key());

        assertEquals("HELD", tx.status());
        assertFalse(tx.holdReasons().isEmpty());
        assertMoney("300000.00", transactionService.balanceOf(aliceAccount));
        assertMoney("0.00", transactionService.balanceOf(bobAccount));
    }

    /**
     * The most important test: 20 transfers of 100 hit an account holding 1000 at the same moment.
     * Without row locking, several threads could read "1000" and all succeed (lost updates).
     * With locking, exactly 10 succeed and the balance ends at exactly 0, never negative.
     */
    @Test
    void concurrentTransfersNeverOverdrawTheAccount() throws Exception {
        deposit(alice, aliceAccount, "1000.00");

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startGate.await(); // all threads start together
                try {
                    transactionService.transfer(alice,
                            new TransferRequest(aliceAccount, bobAccount, new BigDecimal("100.00"), null), key());
                    succeeded.incrementAndGet();
                } catch (ApiException e) {
                    rejected.incrementAndGet(); // insufficient funds
                }
                return null;
            }));
        }
        startGate.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(10, succeeded.get());
        assertEquals(10, rejected.get());
        assertMoney("0.00", transactionService.balanceOf(aliceAccount));
        assertMoney("1000.00", transactionService.balanceOf(bobAccount));
    }

    // ----- helpers -----

    private CurrentUser newCustomer(String name) {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@test.com";
        AuthResponse auth = authService.register(new RegisterRequest(name, email, "Passw0rd123"));
        return new CurrentUser(auth.userId(), email, Role.CUSTOMER);
    }

    private void deposit(CurrentUser user, String account, String amount) {
        transactionService.deposit(user, new AmountRequest(account, new BigDecimal(amount), null), key());
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }
}
