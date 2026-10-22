package com.securebank.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketTriageTest {

    private final TicketClassifier classifier = new TicketClassifier();
    private final UrgencyScorer scorer = new UrgencyScorer();

    @Test
    void classifiesCommonComplaints() {
        assertEquals(TicketCategory.FRAUD_REPORT,
                classifier.classify("There is a withdrawal I never made, I think someone hacked my account").label());
        assertEquals(TicketCategory.CARD_ISSUE,
                classifier.classify("My debit card got declined at the ATM twice").label());
        assertEquals(TicketCategory.FAILED_TRANSFER,
                classifier.classify("Transfer to my landlord is stuck and the money has not reached him").label());
        assertEquals(TicketCategory.PROFILE_UPDATE,
                classifier.classify("Please update my mobile number").label());
    }

    @Test
    void angryUrgentMessageIsUrgent() {
        UrgencyScorer.Assessment a = scorer.assess(
                "This is unacceptable!! I need my money released immediately, it's an emergency",
                TicketCategory.FAILED_TRANSFER);
        assertEquals(Urgency.URGENT, a.urgency());
        assertEquals("UPSET", a.tone());
    }

    @Test
    void fraudIsAlwaysUrgentAndCalmQuestionIsNormal() {
        assertEquals(Urgency.URGENT, scorer.assess("I see a payment I don't recognise", TicketCategory.FRAUD_REPORT).urgency());

        UrgencyScorer.Assessment calm = scorer.assess("How do I change my registered email?", TicketCategory.PROFILE_UPDATE);
        assertEquals(Urgency.NORMAL, calm.urgency());
        assertEquals("CALM", calm.tone());
    }
}
