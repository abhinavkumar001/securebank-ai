package com.securebank.transaction;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_from", columnList = "fromAccountNumber"),
        @Index(name = "idx_tx_to", columnList = "toAccountNumber"),
        @Index(name = "idx_tx_created", columnList = "createdAt")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public reference shown to customers, so internal ids are never exposed. */
    @Column(nullable = false, unique = true, length = 36)
    private String reference;

    /**
     * Sent by the client in the Idempotency-Key header.
     * The UNIQUE constraint guarantees one key = one transaction, even under retries.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /** Null for deposits. */
    @Column(length = 20)
    private String fromAccountNumber;

    /** Null for withdrawals. */
    @Column(length = 20)
    private String toAccountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 200)
    private String description;

    /** Spending category, e.g. Housing, Groceries. Set automatically by TransactionCategorizer. */
    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false)
    private Long initiatedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Transaction() {
    }

    public Transaction(String idempotencyKey, TransactionType type, TransactionStatus status,
                       String fromAccountNumber, String toAccountNumber, BigDecimal amount,
                       String description, String category, Long initiatedBy) {
        this.reference = UUID.randomUUID().toString();
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.status = status;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.initiatedBy = initiatedBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    /** A held transfer that a reviewer approved: the money has now moved. */
    public void markApproved() {
        requireHeld();
        status = TransactionStatus.SUCCESS;
    }

    /** A held transfer that a reviewer rejected: the money never moves. */
    public void markRejected() {
        requireHeld();
        status = TransactionStatus.REJECTED;
    }

    private void requireHeld() {
        if (status != TransactionStatus.HELD) {
            throw new IllegalStateException("Only HELD transactions can be reviewed, this one is " + status);
        }
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public String getFromAccountNumber() { return fromAccountNumber; }
    public String getToAccountNumber() { return toAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public Long getInitiatedBy() { return initiatedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
