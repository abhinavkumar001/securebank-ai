package com.securebank.support;

import com.securebank.ai.TextTokenizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Lexicon-based tone and urgency scoring. Each signal word adds weight; the total decides
 * whether the ticket jumps the queue. Fraud reports are always urgent.
 */
@Component
public class UrgencyScorer {

    public record Assessment(Urgency urgency, double score, String tone) {}

    // Stemmed by TextTokenizer, so "frustrated" matches "frustrat".
    private static final Map<String, Double> URGENCY_WORDS = Map.ofEntries(
            Map.entry("urgent", 0.35), Map.entry("urgently", 0.35), Map.entry("immediately", 0.35),
            Map.entry("asap", 0.35), Map.entry("emergency", 0.4), Map.entry("today", 0.2), Map.entry("now", 0.15),
            Map.entry("stuck", 0.2), Map.entry("deadline", 0.25), Map.entry("hospital", 0.3), Map.entry("rent", 0.1));

    private static final Map<String, Double> NEGATIVE_WORDS = Map.ofEntries(
            Map.entry("angry", 0.3), Map.entry("frustrat", 0.3), Map.entry("worst", 0.35), Map.entry("terrible", 0.3),
            Map.entry("unacceptable", 0.35), Map.entry("disappoint", 0.25), Map.entry("ridiculous", 0.3),
            Map.entry("useless", 0.3), Map.entry("pathetic", 0.35), Map.entry("fed", 0.15), Map.entry("worri", 0.2),
            Map.entry("upset", 0.25), Map.entry("again", 0.1), Map.entry("still", 0.1), Map.entry("never", 0.1));

    public Assessment assess(String message, TicketCategory category) {
        List<String> tokens = TextTokenizer.tokenize(message);
        double urgency = 0;
        double negativity = 0;
        for (String t : tokens) {
            urgency += URGENCY_WORDS.getOrDefault(t, 0.0);
            negativity += NEGATIVE_WORDS.getOrDefault(t, 0.0);
        }
        long exclamations = message.chars().filter(c -> c == '!').count();
        negativity += Math.min(exclamations, 3) * 0.1;
        if (message.length() > 12 && message.equals(message.toUpperCase())) {
            negativity += 0.3; // ALL CAPS
        }

        double score = Math.min(1.0, urgency + negativity);
        String tone = negativity >= 0.3 ? "UPSET" : negativity > 0 ? "CONCERNED" : "CALM";

        Urgency level;
        if (category == TicketCategory.FRAUD_REPORT || score >= 0.6) {
            level = Urgency.URGENT;
        } else if (score >= 0.3 || category == TicketCategory.FAILED_TRANSFER) {
            level = Urgency.HIGH;
        } else {
            level = Urgency.NORMAL;
        }
        return new Assessment(level, Math.round(score * 100) / 100.0, tone);
    }
}
