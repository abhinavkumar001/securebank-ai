package com.securebank.transaction;

import com.securebank.account.Account;
import com.securebank.account.AccountRepository;
import com.securebank.account.AccountService;
import com.securebank.audit.AuditService;
import com.securebank.common.ApiException;
import com.securebank.common.CurrentUser;
import com.securebank.common.PageResponse;
import com.securebank.fraud.FraudAlert;
import com.securebank.fraud.FraudAlertRepository;
import com.securebank.fraud.FraudAlertStatus;
import com.securebank.fraud.FraudChecker;
import com.securebank.fraud.FraudDecision;
import com.securebank.transaction.dto.AmountRequest;
import com.securebank.transaction.dto.TransactionResponse;
import com.securebank.transaction.dto.TransferRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * All money movement goes through here. Three guarantees:
 *  1. Atomic     - @Transactional: debit and credit both happen, or neither does.
 *  2. Isolated   - accounts are row-locked (SELECT ... FOR UPDATE) in a fixed order,
 *                  so concurrent transfers can't corrupt balances or deadlock.
 *  3. Idempotent - the same Idempotency-Key never moves money twice.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final FraudChecker fraudChecker;
    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionCategorizer categorizer;
    private final AuditService auditService;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository,
                              AccountService accountService, FraudChecker fraudChecker,
                              FraudAlertRepository fraudAlertRepository, TransactionCategorizer categorizer,
                              AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.fraudChecker = fraudChecker;
        this.fraudAlertRepository = fraudAlertRepository;
        this.categorizer = categorizer;
        this.auditService = auditService;
    }

    @Transactional
    public TransactionResponse deposit(CurrentUser user, AmountRequest request, String idempotencyKey) {
        Optional<TransactionResponse> replay = findReplay(user, idempotencyKey);
        if (replay.isPresent()) {
            return replay.get();
        }

        Account account = lockOwnedAccount(user, request.accountNumber());
        account.credit(request.amount());

        Transaction tx = transactionRepository.save(new Transaction(idempotencyKey, TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS, null, account.getAccountNumber(), request.amount(),
                request.description(), categorizer.categorize(TransactionType.DEPOSIT, request.description()),
                user.id()));
        auditService.log(user.id(), "DEPOSIT", "Transaction", tx.getReference(),
                request.amount() + " to " + account.getAccountNumber());
        return TransactionResponse.from(tx);
    }

    @Transactional
    public TransactionResponse withdraw(CurrentUser user, AmountRequest request, String idempotencyKey) {
        Optional<TransactionResponse> replay = findReplay(user, idempotencyKey);
        if (replay.isPresent()) {
            return replay.get();
        }

        Account account = lockOwnedAccount(user, request.accountNumber());
        account.debit(request.amount());

        Transaction tx = transactionRepository.save(new Transaction(idempotencyKey, TransactionType.WITHDRAWAL,
                TransactionStatus.SUCCESS, account.getAccountNumber(), null, request.amount(),
                request.description(), categorizer.categorize(TransactionType.WITHDRAWAL, request.description()),
                user.id()));
        auditService.log(user.id(), "WITHDRAWAL", "Transaction", tx.getReference(),
                request.amount() + " from " + account.getAccountNumber());
        return TransactionResponse.from(tx);
    }

    @Transactional
    public TransactionResponse transfer(CurrentUser user, TransferRequest request, String idempotencyKey) {
        Optional<TransactionResponse> replay = findReplay(user, idempotencyKey);
        if (replay.isPresent()) {
            return replay.get();
        }
        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw ApiException.badRequest("Cannot transfer to the same account");
        }

        LockedPair pair = lockPair(request.fromAccountNumber(), request.toAccountNumber());
        Account from = pair.from();
        Account to = pair.to();

        if (!from.isOwnedBy(user.id())) {
            throw ApiException.notFound("Account not found");
        }
        from.ensureActive();
        to.ensureActive();
        if (from.getBalance().compareTo(request.amount()) < 0) {
            throw ApiException.badRequest("Insufficient funds in account " + from.getAccountNumber());
        }

        String category = categorizer.categorize(TransactionType.TRANSFER, request.description());
        FraudDecision decision = fraudChecker.check(from, to, request.amount());
        if (decision.hold()) {
            Transaction held = transactionRepository.save(new Transaction(idempotencyKey, TransactionType.TRANSFER,
                    TransactionStatus.HELD, from.getAccountNumber(), to.getAccountNumber(), request.amount(),
                    request.description(), category, user.id()));
            fraudAlertRepository.save(new FraudAlert(held.getReference(), user.id(), from.getAccountNumber(),
                    to.getAccountNumber(), request.amount(), decision));
            auditService.log(user.id(), "TRANSFER_HELD", "Transaction", held.getReference(),
                    "Risk " + decision.riskScore() + ": " + String.join("; ", decision.messages()));
            return TransactionResponse.from(held, decision.riskScore(), decision.messages());
        }

        from.debit(request.amount());
        to.credit(request.amount());

        Transaction tx = transactionRepository.save(new Transaction(idempotencyKey, TransactionType.TRANSFER,
                TransactionStatus.SUCCESS, from.getAccountNumber(), to.getAccountNumber(), request.amount(),
                request.description(), category, user.id()));
        auditService.log(user.id(), "TRANSFER", "Transaction", tx.getReference(),
                request.amount() + " from " + from.getAccountNumber() + " to " + to.getAccountNumber());
        return TransactionResponse.from(tx, decision.riskScore(), decision.messages());
    }

    // ----- admin review of held transfers -----

    /** Approve a held transfer: re-check everything under lock, then move the money. */
    @Transactional
    public TransactionResponse approveHeld(CurrentUser admin, Long alertId, String note) {
        FraudAlert alert = pendingAlert(alertId);
        Transaction tx = transactionRepository.findByReference(alert.getTransactionReference())
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        LockedPair pair = lockPair(tx.getFromAccountNumber(), tx.getToAccountNumber());
        pair.from().ensureActive();
        pair.to().ensureActive();
        // The balance may have changed since the transfer was held.
        pair.from().debit(tx.getAmount());
        pair.to().credit(tx.getAmount());

        tx.markApproved();
        alert.resolve(FraudAlertStatus.APPROVED, admin.id(), note);
        auditService.log(admin.id(), "TRANSFER_APPROVED", "Transaction", tx.getReference(),
                tx.getAmount() + " released" + (note == null || note.isBlank() ? "" : ": " + note));
        return TransactionResponse.from(tx);
    }

    @Transactional
    public TransactionResponse rejectHeld(CurrentUser admin, Long alertId, String note) {
        FraudAlert alert = pendingAlert(alertId);
        Transaction tx = transactionRepository.findByReference(alert.getTransactionReference())
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        tx.markRejected();
        alert.resolve(FraudAlertStatus.REJECTED, admin.id(), note);
        auditService.log(admin.id(), "TRANSFER_REJECTED", "Transaction", tx.getReference(),
                tx.getAmount() + " blocked" + (note == null || note.isBlank() ? "" : ": " + note));
        return TransactionResponse.from(tx);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> statement(CurrentUser user, String accountNumber, int page, int size) {
        accountService.getAccessible(user, accountNumber); // ownership check
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return PageResponse.from(transactionRepository.findStatement(accountNumber, pageable)
                .map(TransactionResponse::from));
    }

    /** Used by tests and dashboards. */
    @Transactional(readOnly = true)
    public BigDecimal balanceOf(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(Account::getBalance)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
    }

    // ----- helpers -----

    private record LockedPair(Account from, Account to) {}

    /**
     * Always lock the two accounts in the same (alphabetical) order.
     * If request A locks X then Y while request B locks Y then X, they deadlock;
     * a fixed order makes that impossible.
     */
    private LockedPair lockPair(String fromNumber, String toNumber) {
        boolean fromFirst = fromNumber.compareTo(toNumber) < 0;
        Account first = lock(fromFirst ? fromNumber : toNumber);
        Account second = lock(fromFirst ? toNumber : fromNumber);
        return fromFirst ? new LockedPair(first, second) : new LockedPair(second, first);
    }

    private FraudAlert pendingAlert(Long alertId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> ApiException.notFound("Review item not found"));
        if (alert.getStatus() != FraudAlertStatus.PENDING) {
            throw ApiException.conflict("This transfer was already " + alert.getStatus().name().toLowerCase());
        }
        return alert;
    }

    /**
     * If this key was already used, return the original result instead of moving money again.
     * This makes "client timed out and retried" safe.
     */
    private Optional<TransactionResponse> findReplay(CurrentUser user, String idempotencyKey) {
        validateKey(idempotencyKey);
        return transactionRepository.findByIdempotencyKey(idempotencyKey).map(existing -> {
            if (!existing.getInitiatedBy().equals(user.id())) {
                throw ApiException.conflict("Idempotency-Key already used");
            }
            return TransactionResponse.from(existing);
        });
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 64) {
            throw ApiException.badRequest("Idempotency-Key header is required (max 64 characters)");
        }
    }

    private Account lock(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Account not found: " + accountNumber));
    }

    private Account lockOwnedAccount(CurrentUser user, String accountNumber) {
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        if (!account.isOwnedBy(user.id())) {
            throw ApiException.notFound("Account not found");
        }
        return account;
    }
}
