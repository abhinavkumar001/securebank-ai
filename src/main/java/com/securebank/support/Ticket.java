package com.securebank.support;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "support_tickets", indexes = {
        @Index(name = "idx_ticket_user", columnList = "userId"),
        @Index(name = "idx_ticket_status", columnList = "status")
})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketCategory category;

    @Column(nullable = false)
    private double categoryConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgency urgency;

    @Column(nullable = false)
    private double urgencyScore;

    @Column(nullable = false, length = 15)
    private String tone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketStatus status;

    @Column(length = 500)
    private String resolution;

    private Long resolvedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    protected Ticket() {
    }

    public Ticket(Long userId, String message, TicketCategory category, double categoryConfidence,
                  UrgencyScorer.Assessment assessment) {
        this.userId = userId;
        this.message = message;
        this.category = category;
        this.categoryConfidence = categoryConfidence;
        this.urgency = assessment.urgency();
        this.urgencyScore = assessment.score();
        this.tone = assessment.tone();
        this.status = TicketStatus.OPEN;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void resolve(Long adminId, String resolution) {
        this.status = TicketStatus.RESOLVED;
        this.resolvedBy = adminId;
        this.resolution = resolution;
        this.resolvedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getMessage() { return message; }
    public TicketCategory getCategory() { return category; }
    public double getCategoryConfidence() { return categoryConfidence; }
    public Urgency getUrgency() { return urgency; }
    public double getUrgencyScore() { return urgencyScore; }
    public String getTone() { return tone; }
    public TicketStatus getStatus() { return status; }
    public String getResolution() { return resolution; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
