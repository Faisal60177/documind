package com.documind.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

    @Test
    void numbersPassagesAndIncludesTheirSourceAndTheQuestion() {
        UUID document = UUID.randomUUID();
        List<RetrievedChunk> chunks = List.of(
                new RetrievedChunk(UUID.randomUUID(), document, "notes.pdf", 2, 2, "First passage.", 0.9),
                new RetrievedChunk(UUID.randomUUID(), document, "notes.pdf", 4, 5, "Second passage.", 0.8));

        String prompt = PromptBuilder.userPrompt("What is it?", chunks);

        assertThat(prompt).contains("[1] Document: notes.pdf, page 2\nFirst passage.");
        assertThat(prompt).contains("[2] Document: notes.pdf, pages 4-5\nSecond passage.");
        assertThat(prompt).endsWith("Question: What is it?");
    }
}
