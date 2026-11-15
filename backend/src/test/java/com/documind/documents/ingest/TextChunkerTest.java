package com.documind.documents.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    // Bangla: "I sing in Bangla." ending with the danda (U+0964).
    private static final String BANGLA = "\u0986\u09ae\u09bf \u09ac\u09be\u0982\u09b2\u09be\u09df \u0997\u09be\u09a8 \u0997\u09be\u0987\u0964";

    @Test
    void returnsNoChunksForEmptyInput() {
        assertThat(new TextChunker(800, 100).chunk(List.of())).isEmpty();
    }

    @Test
    void keepsShortTextInOneChunkWithItsPage() {
        List<TextChunk> chunks =
                new TextChunker(800, 100).chunk(List.of(new PageText(3, "Hello world. This is a test.")));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().pageStart()).isEqualTo(3);
        assertThat(chunks.getFirst().pageEnd()).isEqualTo(3);
        assertThat(chunks.getFirst().content()).contains("Hello world.", "This is a test.");
    }

    @Test
    void neverExceedsTheTokenLimitAndNumbersChunksInOrder() {
        List<TextChunk> chunks = new TextChunker(200, 20).chunk(List.of(new PageText(1, sentences(400))));

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.tokens()).isLessThanOrEqualTo(200));
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).index()).isEqualTo(i);
        }
    }

    @Test
    void repeatsTheEndOfOneChunkAtTheStartOfTheNext() {
        List<TextChunk> chunks = new TextChunker(200, 20).chunk(List.of(new PageText(1, sentences(400))));

        String first = chunks.get(0).content();
        String lastSentence = first.substring(first.lastIndexOf("Sentence number"));
        assertThat(chunks.get(1).content()).contains(lastSentence);
    }

    @Test
    void tracksPagesAcrossChunks() {
        List<PageText> pages = List.of(new PageText(1, sentences(60)), new PageText(2, sentences(60)));

        List<TextChunk> chunks = new TextChunker(200, 20).chunk(pages);

        assertThat(chunks.getFirst().pageStart()).isEqualTo(1);
        assertThat(chunks.getLast().pageEnd()).isEqualTo(2);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.pageStart()).isLessThanOrEqualTo(chunk.pageEnd()));
    }

    @Test
    void splitsBanglaTextAtTheDanda() {
        String text = IntStream.range(0, 100).mapToObj(i -> BANGLA).collect(Collectors.joining(" "));

        List<TextChunk> chunks = new TextChunker(100, 10).chunk(List.of(new PageText(1, text)));

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.tokens()).isLessThanOrEqualTo(100);
            assertThat(chunk.content()).endsWith("\u0964");
        });
    }

    @Test
    void splitsLongTextWithoutPunctuation() {
        String text = "word ".repeat(2000).strip();

        List<TextChunk> chunks = new TextChunker(100, 10).chunk(List.of(new PageText(1, text)));

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.tokens()).isLessThanOrEqualTo(100));
    }

    private static String sentences(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> "Sentence number " + i + " is here.")
                .collect(Collectors.joining(" "));
    }
}
