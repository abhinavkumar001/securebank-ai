package com.securebank.ai;

import java.util.Arrays;

/**
 * Binary logistic regression trained with batch gradient descent and L2 regularisation,
 * written from scratch so every step can be explained in an interview.
 *
 *   p(fraud | x) = sigmoid(w·x + b)
 *
 * Because the model is linear, each feature's contribution w_i * x_i explains
 * exactly how much it pushed the score up or down. That's what the reason codes use.
 */
public final class LogisticRegression {

    private final double[] weights;
    private final double bias;

    private LogisticRegression(double[] weights, double bias) {
        this.weights = weights;
        this.bias = bias;
    }

    public static LogisticRegression train(double[][] x, int[] y, double learningRate, int epochs, double l2) {
        if (x.length == 0 || x.length != y.length) {
            throw new IllegalArgumentException("Training data is empty or mismatched");
        }
        int n = x.length;
        int d = x[0].length;
        double[] w = new double[d];
        double b = 0.0;

        // Fraud is rare, so weight the positive class up; otherwise the model learns "always say no".
        long positives = Arrays.stream(y).filter(v -> v == 1).count();
        double positiveWeight = positives == 0 ? 1.0 : (double) (n - positives) / positives;

        for (int epoch = 0; epoch < epochs; epoch++) {
            double[] gradW = new double[d];
            double gradB = 0.0;
            double totalWeight = 0.0;
            for (int i = 0; i < n; i++) {
                double sampleWeight = y[i] == 1 ? positiveWeight : 1.0;
                double error = (sigmoid(dot(w, x[i]) + b) - y[i]) * sampleWeight;
                for (int j = 0; j < d; j++) {
                    gradW[j] += error * x[i][j];
                }
                gradB += error;
                totalWeight += sampleWeight;
            }
            for (int j = 0; j < d; j++) {
                w[j] -= learningRate * (gradW[j] / totalWeight + l2 * w[j]);
            }
            b -= learningRate * gradB / totalWeight;
        }
        return new LogisticRegression(w, b);
    }

    public double predict(double[] x) {
        return sigmoid(dot(weights, x) + bias);
    }

    /** w_i * x_i for every feature: positive values pushed the score towards fraud. */
    public double[] contributions(double[] x) {
        double[] c = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            c[i] = weights[i] * x[i];
        }
        return c;
    }

    public double[] weights() {
        return weights.clone();
    }

    public double bias() {
        return bias;
    }

    private static double dot(double[] a, double[] b) {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    private static double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }
}
