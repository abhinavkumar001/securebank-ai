package com.securebank.support;

import com.securebank.ai.NaiveBayesClassifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Routes support messages to a category using Naive Bayes trained on TicketTrainingData. */
@Component
public class TicketClassifier {

    private final NaiveBayesClassifier<TicketCategory> classifier = new NaiveBayesClassifier<>();

    public TicketClassifier() {
        List<String> texts = new ArrayList<>();
        List<TicketCategory> labels = new ArrayList<>();
        TicketTrainingData.EXAMPLES.forEach((category, examples) -> examples.forEach(text -> {
            texts.add(text);
            labels.add(category);
        }));
        classifier.train(texts, labels);
    }

    public NaiveBayesClassifier.Prediction<TicketCategory> classify(String message) {
        return classifier.predict(message);
    }
}
