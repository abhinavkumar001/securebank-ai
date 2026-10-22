package com.securebank.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextAiTest {

    @Test
    void tokenizerLowercasesRemovesStopWordsAndStems() {
        assertEquals(List.of("transfer", "held"), TextTokenizer.tokenize("Why are my Transfers HELD?"));
        assertEquals("transfer", TextTokenizer.stem("transferring"));
        assertEquals("transfer", TextTokenizer.stem("transferred"));
    }

    @Test
    void tfIdfFindsTheMostRelevantDocument() {
        TfIdfIndex index = new TfIdfIndex(List.of(
                "how to freeze a lost debit card",
                "interest rate on savings account",
                "transfer held for security review"));

        List<TfIdfIndex.Match> result = index.search("my card was stolen, freeze it", 3);

        assertEquals(0, result.get(0).index());
        assertTrue(index.search("quantum physics", 3).isEmpty());
    }

    @Test
    void naiveBayesLearnsCategories() {
        NaiveBayesClassifier<String> nb = new NaiveBayesClassifier<>();
        nb.train(List.of("card declined at atm", "card blocked lost", "transfer failed money not received",
                        "payment stuck pending transfer"),
                List.of("CARD", "CARD", "TRANSFER", "TRANSFER"));

        NaiveBayesClassifier.Prediction<String> p = nb.predict("my transfer is stuck");
        assertEquals("TRANSFER", p.label());
        assertTrue(p.confidence() > 0.5);
    }

    @Test
    void logisticRegressionSeparatesSimpleData() {
        double[][] x = {{0}, {0.2}, {0.4}, {2.6}, {2.8}, {3.0}};
        int[] y = {0, 0, 0, 1, 1, 1};
        LogisticRegression lr = LogisticRegression.train(x, y, 0.5, 2000, 0.0);

        assertTrue(lr.predict(new double[]{0.1}) < 0.2);
        assertTrue(lr.predict(new double[]{2.9}) > 0.8);
        assertTrue(lr.weights()[0] > 0);
    }
}
