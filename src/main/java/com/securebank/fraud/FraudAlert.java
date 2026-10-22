package com.securebank.fraud;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** A held transfer waiting for (or after) human review. */
@Entity
@Table(name = "fraud_alerts", indexes = {
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_created", columnList = "createdAt")
})
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String transactionReference;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String fromAccountNumber;

    @Column(nullable = false, length = 20)
    private String toAccountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private double riskScore;

    /** Comma-separated codes, e.g. "HIGH_VALUE,NEW_RECIPIENT", used for reporting. */
    @Column(nullable = false, length = 200)
    private String reasonCodes;

    /** Reviewer-friendly sentences, one per line. */
    @Column(nullable = false, length = 1000)
    private String reasonMessages;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudAlertStatus status;

    private Long reviewedBy;

    @Column(length = 300)
    private String reviewNote;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant reviewedAt;

    protected FraudAlert() {
    }

    public FraudAlert(String transactionReference, Long userId, String fromAccountNumber, String toAccountNumber,
                      BigDecimal amount, FraudDecision decision) {
        this.transactionReference = transactionReference;
        this.userId = userId;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.riskScore = decision.riskScore();
        this.reasonCodes = String.join(",", decision.reasons().stream().map(FraudReason::code).toList());
        this.reasonMessages = String.join("\n", decision.messages());
        this.status = FraudAlertStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void resolve(FraudAlertStatus outcome, Long reviewer, String note) {
        if (status != FraudAlertStatus.PENDING) {
            throw new IllegalStateException("Alert already " + status);
        }
        this.status = outcome;
        this.reviewedBy = reviewer;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public List<String> reasonCodeList() {
        return reasonCodes.isBlank() ? List.of() : Arrays.asList(reasonCodes.split(","));
    }

    public List<String> reasonMessageList() {
        return reasonMessages.isBlank() ? List.of() : Arrays.asList(reasonMessages.split("\n"));
    }

    public Long getId() { return id; }
    public String getTransactionReference() { return transactionReference; }
    public Long getUserId() { return userId; }
    public String getFromAccountNumber() { return fromAccountNumber; }
    public String getToAccountNumber() { return toAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public double getRiskScore() { return riskScore; }
    public FraudAlertStatus getStatus() { return status; }
    public Long getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
