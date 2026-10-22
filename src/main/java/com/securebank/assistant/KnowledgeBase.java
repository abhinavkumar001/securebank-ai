package com.securebank.assistant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/** Loads the bank's policy Q&A from src/main/resources/knowledge/faq.json. */
@Component
public class KnowledgeBase {

    public record Entry(String id, String question, String answer, String source) {}

    private final List<Entry> entries;

    public KnowledgeBase(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("knowledge/faq.json").getInputStream()) {
            this.entries = List.copyOf(objectMapper.readValue(in, new TypeReference<List<Entry>>() {}));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load knowledge/faq.json", e);
        }
    }

    public List<Entry> entries() {
        return entries;
    }
}
