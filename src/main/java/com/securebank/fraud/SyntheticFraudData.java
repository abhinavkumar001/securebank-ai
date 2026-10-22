package com.securebank.fraud;

import java.util.Random;

/**
 * Generates labelled training data for the fraud model.
 *
 * A real bank trains on its own labelled history. A new portfolio project has none,
 * so we simulate two behaviours drawn from patterns reported in fraud research:
 * normal customers send familiar amounts to known people at normal hours;
 * fraudulent transfers are unusually large, go to new recipients, happen at odd hours,
 * come in bursts and try to drain the account.
 *
 * The distributions overlap on purpose, so the model has to learn, not memorise a rule.
 */
public final class SyntheticFraudData {

    public record Dataset(double[][] x, int[] y) {}

    private SyntheticFraudData() {
    }

    public static Dataset generate(int size, double fraudRate, long seed) {
        Random r = new Random(seed);
        double[][] x = new double[size][];
        int[] y = new int[size];
        for (int i = 0; i < size; i++) {
            boolean fraud = r.nextDouble() < fraudRate;
            FraudFeatures f = fraud ? fraudulent(r) : normal(r);
            x[i] = f.toVector();
            y[i] = fraud ? 1 : 0;
        }
        return new Dataset(x, y);
    }

    private static FraudFeatures normal(Random r) {
        return new FraudFeatures(
                Math.exp(r.nextGaussian() * 0.6),       // usually close to the average amount
                r.nextDouble() < 0.25,                   // mostly known recipients
                r.nextDouble() < 0.05,                   // rarely at night
                r.nextDouble() < 0.85 ? 0 : 1 + r.nextInt(2),
                r.nextDouble() * 0.45);                  // a small part of the balance
    }

    private static FraudFeatures fraudulent(Random r) {
        return new FraudFeatures(
                Math.exp(1.6 + r.nextGaussian() * 0.8),  // several times the usual amount
                r.nextDouble() < 0.85,
                r.nextDouble() < 0.40,
                r.nextDouble() < 0.4 ? 0 : 2 + r.nextInt(5),
                0.4 + r.nextDouble() * 0.6);             // most of the balance
    }
}
