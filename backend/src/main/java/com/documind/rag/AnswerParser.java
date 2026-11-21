package com.documind.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns the model's text into a found/not-found result with clean, sequential citations. */
final class AnswerParser {

    private static final Pattern CITATION = Pattern.compile("\\[(\\d{1,3}(?:\\s*,\\s*\\d{1,3})*)\\]");

    private AnswerParser() {}

    static ParsedAnswer parse(String raw, int contextSize) {
        String text = raw == null ? "" : raw.strip();

        if (text.startsWith(PromptBuilder.NOT_FOUND_MARKER)) {
            String message = text.substring(PromptBuilder.NOT_FOUND_MARKER.length());
            message = CITATION.matcher(message).replaceAll("").strip();
            return new ParsedAnswer(false, message, java.util.List.of());
        }
        text = text.replace(PromptBuilder.NOT_FOUND_MARKER, "").strip();

        Map<Integer, Integer> renumbered = new LinkedHashMap<>();
        Matcher matcher = CITATION.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            Set<Integer> inGroup = new LinkedHashSet<>();
            for (String part : matcher.group(1).split("\\s*,\\s*")) {
                int original = Integer.parseInt(part);
                if (original < 1 || original > contextSize) {
                    continue; // the model cited a passage that does not exist
                }
                int number = renumbered.computeIfAbsent(original, key -> renumbered.size() + 1);
                inGroup.add(number);
            }
            StringBuilder replacement = new StringBuilder();
            for (int number : inGroup) {
                replacement.append('[').append(number).append(']');
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(out);

        return new ParsedAnswer(true, out.toString().strip(), new ArrayList<>(renumbered.keySet()));
    }
}
