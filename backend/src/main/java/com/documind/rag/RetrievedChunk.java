package com.documind.rag;

import java.util.UUID;

/** A stored passage returned by similarity search. {@code score} is cosine similarity. */
public record RetrievedChunk(
        UUID chunkId,
        UUID documentId,
        String filename,
        int page,
        int pageEnd,
        String content,
        double score) {}
