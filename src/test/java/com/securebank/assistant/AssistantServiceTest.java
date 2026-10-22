package com.securebank.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssistantServiceTest {

    private final AssistantService assistant = new AssistantService(new KnowledgeBase(new ObjectMapper()));

    @Test
    void answersHeldTransferQuestionFromPolicy() {
        AssistantService.Answer a = assistant.ask("why was my transfer put on hold?");
        assertTrue(a.found());
        assertTrue(a.source().startsWith("Funds Transfer Policy"), a.source());
    }

    @Test
    void answersCardQuestionWithDifferentWording() {
        AssistantService.Answer a = assistant.ask("I lost my debit card, how can I block it");
        assertTrue(a.found());
        assertTrue(a.source().startsWith("Card Services"), a.source());
    }

    @Test
    void admitsWhenItDoesNotKnow() {
        AssistantService.Answer a = assistant.ask("who won the cricket match yesterday");
        assertFalse(a.found());
        assertNull(a.source());
        assertFalse(a.related().isEmpty());
    }
}
