package com.documind.documents.ingest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits page text into overlapping passages of at most {@code maxTokens} estimated tokens.
 *
 * <p>Passages are built from whole sentences (English full stops and the Bangla danda), so they
 * read naturally. Tokens are estimated, not counted exactly: about 4 Latin characters per token
 * and roughly 1.7 non-Latin characters per token. That keeps the estimate safe for Bangla.
 */
public class TextChunker {

    private static final Pattern SENTENCE_BOUNDARY =
            Pattern.compile("(?<=[.!?\\u0964\\u0965])\\s+|\\n{2,}");

    private static final long ASCII_WEIGHT = 250;
    private static final long OTHER_WEIGHT = 600;

    private final int maxTokens;
    private final int overlapTokens;

    public TextChunker(int maxTokens, int overlapTokens) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        if (overlapTokens < 0 || overlapTokens >= maxTokens) {
            throw new IllegalArgumentException("overlapTokens must be between 0 and maxTokens - 1");
        }
        this.maxTokens = maxTokens;
        this.overlapTokens = overlapTokens;
    }

    public static int estimateTokens(String text) {
        return (int) ((weight(text) + 999) / 1000);
    }

    public List<TextChunk> chunk(List<PageText> pages) {
        List<Unit> units = new ArrayList<>();
        for (PageText page : pages) {
            for (String sentence : SENTENCE_BOUNDARY.split(page.text())) {
                String trimmed = sentence.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }
                for (String piece : splitOversized(trimmed)) {
                    units.add(new Unit(page.page(), piece, estimateTokens(piece)));
                }
            }
        }

        List<TextChunk> chunks = new ArrayList<>();
        Deque<Unit> window = new ArrayDeque<>();
        int windowTokens = 0;
        int fresh = 0; // units added since the last chunk was emitted

        for (Unit unit : units) {
            if (fresh > 0 && windowTokens + unit.tokens() > maxTokens) {
                chunks.add(build(chunks.size(), window));
                // Keep the tail of this chunk as the overlap for the next one.
                while (!window.isEmpty()
                        && (windowTokens > overlapTokens || windowTokens + unit.tokens() > maxTokens)) {
                    windowTokens -= window.removeFirst().tokens();
                }
                fresh = 0;
            }
            window.addLast(unit);
            windowTokens += unit.tokens();
            fresh++;
        }
        if (fresh > 0) {
            chunks.add(build(chunks.size(), window));
        }
        return chunks;
    }

    private static TextChunk build(int index, Deque<Unit> window) {
        StringBuilder content = new StringBuilder();
        int tokens = 0;
        for (Unit unit : window) {
            if (!content.isEmpty()) {
                content.append(' ');
            }
            content.append(unit.text());
            tokens += unit.tokens();
        }
        return new TextChunk(index, window.peekFirst().page(), window.peekLast().page(), content.toString(), tokens);
    }

    /** Splits a very long run of text (a table, or text without punctuation) into small pieces. */
    private List<String> splitOversized(String text) {
        long limit = maxTokens * 1000L;
        if (weight(text) <= limit) {
            return List.of(text);
        }

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        long currentWeight = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            long w = codePoint < 128 ? ASCII_WEIGHT : OTHER_WEIGHT;
            if (currentWeight + w > limit) {
                int cut = current.lastIndexOf(" ");
                if (cut > current.length() / 2) {
                    String rest = current.substring(cut + 1);
                    parts.add(current.substring(0, cut));
                    current.setLength(0);
                    current.append(rest);
                    currentWeight = weight(rest);
                } else {
                    parts.add(current.toString());
                    current.setLength(0);
                    currentWeight = 0;
                }
            }
            current.appendCodePoint(codePoint);
            currentWeight += w;
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts.stream().map(String::strip).filter(p -> !p.isEmpty()).toList();
    }

    private static long weight(String text) {
        long total = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            total += codePoint < 128 ? ASCII_WEIGHT : OTHER_WEIGHT;
            i += Character.charCount(codePoint);
        }
        return total;
    }

    private record Unit(int page, String text, int tokens) {}
}
