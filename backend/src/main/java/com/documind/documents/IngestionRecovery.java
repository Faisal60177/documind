package com.documind.documents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Marks documents left in PROCESSING by a previous run (crash or restart) as FAILED. */
@Component
class IngestionRecovery {

    private static final Logger log = LoggerFactory.getLogger(IngestionRecovery.class);

    private final DocumentRepository documents;

    IngestionRecovery(DocumentRepository documents) {
        this.documents = documents;
    }

    @EventListener(ApplicationReadyEvent.class)
    void failInterruptedDocuments() {
        int updated = documents.failAllProcessing("Processing was interrupted. Please upload the file again.");
        if (updated > 0) {
            log.warn("Marked {} interrupted document(s) as FAILED", updated);
        }
    }
}
