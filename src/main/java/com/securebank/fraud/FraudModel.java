package com.securebank.fraud;

import com.securebank.ai.LogisticRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trains a logistic regression fraud model on startup and explains each score with reason codes.
 * Training on 4,000 rows takes well under a second.
 */
@Component
public class FraudModel {

    private static final Logger log = LoggerFactory.getLogger(FraudModel.class);
    /** A feature must push the score up by at least this much to be listed as a reason. */
    private static final double REASON_THRESHOLD = 0.4;

    private final LogisticRegression model;
    private final ModelMetrics metrics;

    public FraudModel() {
        this(42L);
    }

    FraudModel(long seed) {
        SyntheticFraudData.Dataset data = SyntheticFraudData.generate(5_000, 0.10, seed);
        int split = (int) (data.x().length * 0.8);
        double[][] trainX = Arrays.copyOfRange(data.x(), 0, split);
        int[] trainY = Arrays.copyOfRange(data.y(), 0, split);
        double[][] testX = Arrays.copyOfRange(data.x(), split, data.x().length);
        int[] testY = Arrays.copyOfRange(data.y(), split, data.y().length);

        this.model = LogisticRegression.train(trainX, trainY, 0.5, 1_500, 0.001);
        this.metrics = evaluate(testX, testY, trainX.length);
        log.info("Fraud model trained: accuracy={}, precision={}, recall={}, f1={}",
                metrics.accuracy(), metrics.precision(), metrics.recall(), metrics.f1());
    }

    public double score(FraudFeatures features) {
        return model.predict(features.toVector());
    }

    /** Turns the biggest positive feature contributions into sentences a reviewer understands. */
    public List<FraudReason> explain(FraudFeatures f) {
        double[] contributions = model.contributions(f.toVector());
        List<Integer> order = new ArrayList<>(List.of(0, 1, 2, 3, 4));
        order.sort((a, b) -> Double.compare(contributions[b], contributions[a]));

        List<FraudReason> reasons = new ArrayList<>();
        for (int i : order) {
            if (contributions[i] < REASON_THRESHOLD) {
                break;
            }
            reasons.add(switch (i) {
                case 0 -> new FraudReason("UNUSUAL_AMOUNT",
                        String.format("Amount is %.1f× this account’s usual transfer", f.amountRatio()));
                case 1 -> new FraudReason("NEW_RECIPIENT", "First transfer to this recipient");
                case 2 -> new FraudReason("ODD_HOUR", "Sent between midnight and 5 am");
                case 3 -> new FraudReason("RAPID_REPEAT",
                        f.recentTransfers() + " transfers from this account in the last 10 minutes");
                default -> new FraudReason("HIGH_BALANCE_SHARE",
                        String.format("Uses %.0f%% of the available balance", f.balanceShare() * 100));
            });
        }
        if (reasons.isEmpty()) {
            reasons.add(new FraudReason("UNUSUAL_PATTERN", "Overall pattern differs from this account’s normal activity"));
        }
        return reasons;
    }

    public ModelMetrics metrics() {
        return metrics;
    }

    private ModelMetrics evaluate(double[][] x, int[] y, int trainSize) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (int i = 0; i < x.length; i++) {
            boolean predicted = model.predict(x[i]) >= 0.5;
            boolean actual = y[i] == 1;
            if (predicted && actual) tp++;
            else if (predicted) fp++;
            else if (actual) fn++;
            else tn++;
        }
        double precision = tp + fp == 0 ? 0 : (double) tp / (tp + fp);
        double recall = tp + fn == 0 ? 0 : (double) tp / (tp + fn);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);

        Map<String, Double> weights = new LinkedHashMap<>();
        double[] w = model.weights();
        for (int i = 0; i < w.length; i++) {
            weights.put(FraudFeatures.NAMES[i], round(w[i]));
        }
        return new ModelMetrics(round((double) (tp + tn) / x.length), round(precision), round(recall), round(f1),
                trainSize, x.length, weights);
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
