package com.securebank.assistant;

import com.securebank.ai.TfIdfIndex;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Retrieval-based assistant: finds the policy entry most similar to the question with TF-IDF
 * and cosine similarity, and answers with its text and source.
 *
 * It never makes up an answer: below the confidence threshold it says it doesn't know
 * and points the customer to a support ticket. This is the "retrieval" half of RAG;
 * an LLM could later rephrase the retrieved answer, but retrieval keeps answers grounded.
 */
@Service
public class AssistantService {

    public record Answer(String answer, String source, double confidence, boolean found, List<String> related) {}

    static final double MIN_CONFIDENCE = 0.12;

    private final KnowledgeBase knowledgeBase;
    private final TfIdfIndex index;

    public AssistantService(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
        // The question is repeated so its words count more than words in the long answer.
        this.index = new TfIdfIndex(knowledgeBase.entries().stream()
                .map(e -> e.question() + " " + e.question() + " " + e.answer())
                .toList());
    }

    public Answer ask(String question) {
        List<TfIdfIndex.Match> matches = index.search(question, 3);
        List<KnowledgeBase.Entry> entries = knowledgeBase.entries();

        if (matches.isEmpty() || matches.get(0).score() < MIN_CONFIDENCE) {
            return new Answer("I couldn’t find that in the bank’s policy documents. Try asking about transfers, "
                    + "holds, cards, payees or statements, or raise a support ticket from Get help.",
                    null, matches.isEmpty() ? 0 : round(matches.get(0).score()), false,
                    List.of(entries.get(0).question(), entries.get(3).question(), entries.get(10).question()));
        }
        KnowledgeBase.Entry best = entries.get(matches.get(0).index());
        List<String> related = matches.stream().skip(1)
                .filter(m -> m.score() >= MIN_CONFIDENCE)
                .map(m -> entries.get(m.index()).question())
                .toList();
        return new Answer(best.answer(), best.source(), round(matches.get(0).score()), true, related);
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
