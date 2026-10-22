package com.securebank.payee;

import jakarta.persistence.*;

import java.time.Instant;

/** A saved beneficiary, so customers don't retype account numbers. */
@Entity
@Table(name = "payees", uniqueConstraints = @UniqueConstraint(
        name = "uk_payee_user_account", columnNames = {"userId", "accountNumber"}))
public class Payee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Payee() {
    }

    public Payee(Long userId, String name, String accountNumber) {
        this.userId = userId;
        this.name = name;
        this.accountNumber = accountNumber;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getAccountNumber() { return accountNumber; }
    public Instant getCreatedAt() { return createdAt; }
}
