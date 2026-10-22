package com.securebank.account;

import com.securebank.account.dto.AccountResponse;
import com.securebank.account.dto.OpenAccountRequest;
import com.securebank.account.dto.UpdateStatusRequest;
import com.securebank.audit.AuditService;
import com.securebank.card.CardService;
import com.securebank.common.ApiException;
import com.securebank.common.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountService {

    private static final int MAX_ACCOUNTS_PER_USER = 5;
    private final SecureRandom random = new SecureRandom();

    private final AccountRepository accountRepository;
    private final AuditService auditService;
    private final CardService cardService;

    public AccountService(AccountRepository accountRepository, AuditService auditService, CardService cardService) {
        this.accountRepository = accountRepository;
        this.auditService = auditService;
        this.cardService = cardService;
    }

    @Transactional
    public AccountResponse open(CurrentUser user, OpenAccountRequest request) {
        if (accountRepository.findByUserIdOrderByCreatedAtAsc(user.id()).size() >= MAX_ACCOUNTS_PER_USER) {
            throw ApiException.badRequest("Maximum of " + MAX_ACCOUNTS_PER_USER + " accounts per customer");
        }
        Account account = accountRepository.save(new Account(newAccountNumber(), user.id(), request.type()));
        cardService.issueFor(user.id(), account.getAccountNumber());
        auditService.log(user.id(), "OPEN_ACCOUNT", "Account", account.getAccountNumber(),
                "Opened " + request.type() + " account");
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listMine(CurrentUser user) {
        return accountRepository.findByUserIdOrderByCreatedAtAsc(user.id()).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAll() {
        return accountRepository.findAll().stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(CurrentUser user, String accountNumber) {
        return AccountResponse.from(getAccessible(user, accountNumber));
    }

    @Transactional
    public AccountResponse updateStatus(CurrentUser admin, String accountNumber, UpdateStatusRequest request) {
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        AccountStatus old = account.getStatus();
        account.changeStatus(request.status());
        auditService.log(admin.id(), "CHANGE_ACCOUNT_STATUS", "Account", accountNumber,
                old + " -> " + request.status() + ": " + request.reason());
        return AccountResponse.from(account);
    }

    /** Customers see only their own accounts; admins see all. Used by the transaction module too. */
    public Account getAccessible(CurrentUser user, String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        if (!user.isAdmin() && !account.isOwnedBy(user.id())) {
            // 404 rather than 403, so customers can't probe which account numbers exist.
            throw ApiException.notFound("Account not found");
        }
        return account;
    }

    private String newAccountNumber() {
        String number;
        do {
            number = "SB" + String.format("%010d", random.nextLong(10_000_000_000L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
