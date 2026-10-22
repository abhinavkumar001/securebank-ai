package com.securebank.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small TF-IDF search index with cosine similarity.
 *
 *   tf(t, d)  = count of t in d / words in d
 *   idf(t)    = ln((1 + N) / (1 + df(t))) + 1     (smoothed, like scikit-learn)
 *   score     = cosine(tfidf(query), tfidf(doc))
 *
 * Rare words ("ifsc", "freeze") weigh more than common ones ("account"),
 * so the best match is the document that shares the most distinctive words.
 */
public final class TfIdfIndex {

    public record Match(int index, double score) {}

    private final Map<String, Double> idf = new HashMap<>();
    private final List<Map<String, Double>> documentVectors = new ArrayList<>();

    public TfIdfIndex(List<String> documents) {
        List<List<String>> tokenized = documents.stream().map(TextTokenizer::tokenize).toList();

        Map<String, Integer> documentFrequency = new HashMap<>();
        for (List<String> tokens : tokenized) {
            tokens.stream().distinct().forEach(t -> documentFrequency.merge(t, 1, Integer::sum));
        }
        int n = documents.size();
        documentFrequency.forEach((term, df) -> idf.put(term, Math.log((1.0 + n) / (1.0 + df)) + 1.0));

        for (List<String> tokens : tokenized) {
            documentVectors.add(weigh(tokens));
        }
    }

    public List<Match> search(String query, int topK) {
        Map<String, Double> q = weigh(TextTokenizer.tokenize(query));
        if (q.isEmpty()) {
            return List.of();
        }
        List<Match> matches = new ArrayList<>();
        for (int i = 0; i < documentVectors.size(); i++) {
            double score = cosine(q, documentVectors.get(i));
            if (score > 0) {
                matches.add(new Match(i, score));
            }
        }
        matches.sort(Comparator.comparingDouble(Match::score).reversed());
        return matches.subList(0, Math.min(topK, matches.size()));
    }

    private Map<String, Double> weigh(List<String> tokens) {
        Map<String, Double> vector = new HashMap<>();
        if (tokens.isEmpty()) {
            return vector;
        }
        for (String t : tokens) {
            vector.merge(t, 1.0, Double::sum);
        }
        // Words never seen in the corpus can't match anything, so they're dropped.
        vector.replaceAll((term, count) -> idf.containsKey(term) ? (count / tokens.size()) * idf.get(term) : 0.0);
        vector.values().removeIf(v -> v == 0.0);
        return vector;
    }

    private static double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double other = b.get(e.getKey());
            if (other != null) {
                dot += e.getValue() * other;
            }
        }
        if (dot == 0) {
            return 0;
        }
        return dot / (norm(a) * norm(b));
    }

    private static double norm(Map<String, Double> v) {
        double s = 0;
        for (double x : v.values()) {
            s += x * x;
        }
        return Math.sqrt(s);
    }
}
