package com.securebank.card;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A debit card linked to one account. Only the first 4 and last 4 digits are stored,
 * the way real systems avoid keeping full card numbers (PCI DSS).
 */
@Entity
@Table(name = "cards", indexes = @Index(name = "idx_card_user", columnList = "userId"))
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 4)
    private String firstFour;

    @Column(nullable = false, length = 4)
    private String lastFour;

    @Column(nullable = false, length = 100)
    private String holderName;

    @Column(nullable = false, length = 5)
    private String expiry;

    @Column(nullable = false)
    private boolean frozen;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Card() {
    }

    public Card(Long userId, String accountNumber, String firstFour, String lastFour, String holderName, String expiry) {
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.firstFour = firstFour;
        this.lastFour = lastFour;
        this.holderName = holderName;
        this.expiry = expiry;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getAccountNumber() { return accountNumber; }
    public String getFirstFour() { return firstFour; }
    public String getLastFour() { return lastFour; }
    public String getHolderName() { return holderName; }
    public String getExpiry() { return expiry; }
    public boolean isFrozen() { return frozen; }
}
