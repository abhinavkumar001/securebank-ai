package com.securebank.account;

import com.securebank.common.ApiException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts", indexes = @Index(name = "idx_account_user", columnList = "userId"))
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    /** BigDecimal, never double: floating point can't represent money exactly. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    /** Optimistic locking: a second safety net if two updates ever race outside a row lock. */
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(String accountNumber, Long userId, AccountType type) {
        this.accountNumber = accountNumber;
        this.userId = userId;
        this.type = type;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.status = AccountStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // ----- Business rules live on the entity, so they can't be bypassed -----

    public void credit(BigDecimal amount) {
        ensureActive();
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        ensureActive();
        if (balance.compareTo(amount) < 0) {
            throw ApiException.badRequest("Insufficient funds in account " + accountNumber);
        }
        balance = balance.subtract(amount);
    }

    public void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw ApiException.badRequest("Account " + accountNumber + " is " + status);
        }
    }

    public boolean isOwnedBy(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public void changeStatus(AccountStatus newStatus) {
        this.status = newStatus;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public Long getUserId() { return userId; }
    public AccountType getType() { return type; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
