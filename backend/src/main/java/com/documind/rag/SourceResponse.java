package com.documind.rag;

import java.util.UUID;

/** A cited passage. {@code number} matches the [n] marker in the answer text. */
public record SourceResponse(
        int number,
        UUID documentId,
        String filename,
        int page,
        int pageEnd,
        String snippet,
        double score) {}
