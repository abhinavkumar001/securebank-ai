package com.securebank.transaction.dto;

import com.securebank.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionResponse(String reference, String type, String status,
                                  String fromAccountNumber, String toAccountNumber,
                                  BigDecimal amount, String description, String category,
                                  Instant createdAt, double riskScore, List<String> holdReasons) {

    public static TransactionResponse from(Transaction t) {
        return from(t, 0.0, List.of());
    }

    public static TransactionResponse from(Transaction t, double riskScore, List<String> holdReasons) {
        return new TransactionResponse(t.getReference(), t.getType().name(), t.getStatus().name(),
                t.getFromAccountNumber(), t.getToAccountNumber(), t.getAmount(),
                t.getDescription(), t.getCategory(), t.getCreatedAt(), riskScore, holdReasons);
    }
}
