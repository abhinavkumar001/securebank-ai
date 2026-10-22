package com.securebank.ai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multinomial Naive Bayes text classifier with Laplace smoothing.
 *
 *   label = argmax  log P(label) + Σ log P(word | label)
 *
 * Fast to train, works well on short texts like support messages,
 * and the same family of model as the email spam filter project.
 */
public final class NaiveBayesClassifier<L> {

    public record Prediction<L>(L label, double confidence) {}

    private final Map<L, Integer> documentsPerLabel = new HashMap<>();
    private final Map<L, Map<String, Integer>> wordCounts = new HashMap<>();
    private final Map<L, Integer> totalWords = new HashMap<>();
    private final Set<String> vocabulary = new HashSet<>();
    private int totalDocuments;

    public void train(List<String> texts, List<L> labels) {
        if (texts.size() != labels.size()) {
            throw new IllegalArgumentException("texts and labels must be the same size");
        }
        for (int i = 0; i < texts.size(); i++) {
            L label = labels.get(i);
            documentsPerLabel.merge(label, 1, Integer::sum);
            totalDocuments++;
            Map<String, Integer> counts = wordCounts.computeIfAbsent(label, k -> new HashMap<>());
            for (String token : TextTokenizer.tokenize(texts.get(i))) {
                counts.merge(token, 1, Integer::sum);
                totalWords.merge(label, 1, Integer::sum);
                vocabulary.add(token);
            }
        }
    }

    public Prediction<L> predict(String text) {
        if (totalDocuments == 0) {
            throw new IllegalStateException("Classifier has not been trained");
        }
        List<String> tokens = TextTokenizer.tokenize(text);
        Map<L, Double> logScores = new HashMap<>();
        int v = vocabulary.size();
        for (L label : documentsPerLabel.keySet()) {
            double score = Math.log((double) documentsPerLabel.get(label) / totalDocuments);
            Map<String, Integer> counts = wordCounts.get(label);
            int total = totalWords.getOrDefault(label, 0);
            for (String token : tokens) {
                score += Math.log((counts.getOrDefault(token, 0) + 1.0) / (total + v));
            }
            logScores.put(label, score);
        }

        // Softmax over log scores gives a confidence between 0 and 1.
        double max = logScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double sum = logScores.values().stream().mapToDouble(s -> Math.exp(s - max)).sum();
        L best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<L, Double> e : logScores.entrySet()) {
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                best = e.getKey();
            }
        }
        return new Prediction<>(best, Math.exp(bestScore - max) / sum);
    }
}
