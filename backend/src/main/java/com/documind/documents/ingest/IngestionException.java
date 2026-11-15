package com.documind.documents.ingest;

/** A processing failure. The message is safe to show to the end user. */
public class IngestionException extends RuntimeException {

    public IngestionException(String userMessage) {
        super(userMessage);
    }

    public IngestionException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
