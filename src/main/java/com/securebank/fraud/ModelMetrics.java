package com.securebank.fraud;

import java.util.Map;

/** Held-out evaluation of the fraud model, shown on the admin "Fraud insights" page. */
public record ModelMetrics(double accuracy, double precision, double recall, double f1,
                           int trainSize, int testSize, Map<String, Double> featureWeights) {}
