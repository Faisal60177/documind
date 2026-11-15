package com.documind.documents;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        long sizeBytes,
        Integer pageCount,
        int chunkCount,
        String status,
        String errorMessage,
        Instant createdAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getSizeBytes(),
                document.getPageCount(),
                document.getChunkCount(),
                document.getStatus(),
                document.getErrorMessage(),
                document.getCreatedAt());
    }
}
