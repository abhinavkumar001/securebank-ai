package com.securebank.fraud;

import com.securebank.account.Account;
import com.securebank.common.Rupees;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The bank's fraud check: a hard rule plus the ML model, like real banks run them side by side.
 *  - Rule: transfers at or above the threshold always go to review.
 *  - Model: anything scoring at or above hold-score goes to review, with reason codes.
 */
@Service
public class FraudService implements FraudChecker {

    private final FeatureExtractor featureExtractor;
    private final FraudModel model;
    private final BigDecimal highValueThreshold;
    private final double holdScore;
    private final boolean mlEnabled;

    public FraudService(FeatureExtractor featureExtractor, FraudModel model,
                        @Value("${app.fraud.high-value-threshold}") BigDecimal highValueThreshold,
                        @Value("${app.fraud.hold-score:0.8}") double holdScore,
                        @Value("${app.fraud.ml-enabled:true}") boolean mlEnabled) {
        this.featureExtractor = featureExtractor;
        this.model = model;
        this.highValueThreshold = highValueThreshold;
        this.holdScore = holdScore;
        this.mlEnabled = mlEnabled;
    }

    @Override
    public FraudDecision check(Account from, Account to, BigDecimal amount) {
        List<FraudReason> reasons = new ArrayList<>();
        boolean ruleHit = amount.compareTo(highValueThreshold) >= 0;
        if (ruleHit) {
            reasons.add(new FraudReason("HIGH_VALUE",
                    "Amount is at or above the " + Rupees.format(highValueThreshold) + " review threshold"));
        }

        double score = ruleHit ? 0.8 : 0.0;
        boolean modelHit = false;
        if (mlEnabled) {
            FraudFeatures features = featureExtractor.extract(from, to, amount);
            double modelScore = model.score(features);
            score = Math.max(score, modelScore);
            modelHit = modelScore >= holdScore;
            if (ruleHit || modelHit) {
                reasons.addAll(model.explain(features).stream()
                        .filter(r -> !(ruleHit && r.code().equals("UNUSUAL_PATTERN")))
                        .toList());
            }
        }

        double rounded = Math.round(score * 100) / 100.0;
        return ruleHit || modelHit
                ? new FraudDecision(true, rounded, reasons)
                : FraudDecision.allow(rounded);
    }
}
