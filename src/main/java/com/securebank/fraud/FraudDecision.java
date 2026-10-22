package com.securebank.fraud;

import java.util.List;

/**
 * Result of a fraud check.
 * @param hold       true = stop the transfer and send it for admin review
 * @param riskScore  0.0 (safe) to 1.0 (very risky)
 * @param reasons    reason codes shown to the reviewer
 */
public record FraudDecision(boolean hold, double riskScore, List<FraudReason> reasons) {

    public static FraudDecision allow(double riskScore) {
        return new FraudDecision(false, riskScore, List.of());
    }

    public List<String> messages() {
        return reasons.stream().map(FraudReason::message).toList();
    }
}
