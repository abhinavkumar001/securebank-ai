package com.securebank.fraud;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReviewResponse(Long id, String transactionReference, Long userId,
                             String fromAccountNumber, String toAccountNumber, BigDecimal amount,
                             double riskScore, List<String> reasonCodes, List<String> reasons,
                             String status, Long reviewedBy, String reviewNote,
                             Instant createdAt, Instant reviewedAt) {

    public static ReviewResponse from(FraudAlert a) {
        return new ReviewResponse(a.getId(), a.getTransactionReference(), a.getUserId(),
                a.getFromAccountNumber(), a.getToAccountNumber(), a.getAmount(), a.getRiskScore(),
                a.reasonCodeList(), a.reasonMessageList(), a.getStatus().name(),
                a.getReviewedBy(), a.getReviewNote(), a.getCreatedAt(), a.getReviewedAt());
    }
}
