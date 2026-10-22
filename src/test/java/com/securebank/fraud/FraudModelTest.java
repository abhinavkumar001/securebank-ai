package com.securebank.fraud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FraudModelTest {

    private final FraudModel model = new FraudModel(7L);

    @Test
    void modelPerformsWellOnHeldOutData() {
        ModelMetrics m = model.metrics();
        assertTrue(m.recall() > 0.8, "recall " + m.recall());
        assertTrue(m.precision() > 0.6, "precision " + m.precision());
        assertTrue(m.f1() > 0.75, "f1 " + m.f1());
    }

    @Test
    void everydayTransferScoresLow() {
        // usual amount, known recipient, daytime, no burst, 10% of balance
        double score = model.score(new FraudFeatures(1.0, false, false, 0, 0.10));
        assertTrue(score < 0.3, "score " + score);
    }

    @Test
    void drainingTransferAtNightScoresHigh() {
        // 9x usual amount, new recipient, 3 am, 4 transfers in 10 min, 95% of balance
        FraudFeatures risky = new FraudFeatures(9.0, true, true, 4, 0.95);
        assertTrue(model.score(risky) > 0.9, "score " + model.score(risky));

        List<String> codes = model.explain(risky).stream().map(FraudReason::code).toList();
        assertTrue(codes.contains("UNUSUAL_AMOUNT"), codes.toString());
        assertTrue(codes.contains("NEW_RECIPIENT"), codes.toString());
    }

    @Test
    void reasonsReadLikeSentences() {
        FraudReason first = model.explain(new FraudFeatures(8.0, true, false, 0, 0.9)).get(0);
        assertFalse(first.message().isBlank());
        assertFalse(first.message().contains("_"), "messages should be human-readable");
    }
}
