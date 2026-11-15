package com.documind.documents;

import com.documind.documents.ingest.ChunkRepository;
import com.documind.documents.ingest.ExtractedPdf;
import com.documind.documents.ingest.GeminiEmbeddingClient;
import com.documind.documents.ingest.IngestionException;
import com.documind.documents.ingest.PdfTextExtractor;
import com.documind.documents.ingest.TextChunk;
import com.documind.documents.ingest.TextChunker;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Turns an uploaded PDF into embedded chunks: extract, chunk, embed, store. */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final PdfTextExtractor extractor;
    private final TextChunker chunker;
    private final GeminiEmbeddingClient embeddingClient;
    private final ChunkRepository chunkRepository;
    private final DocumentRepository documents;
    private final DocumentProperties properties;
    private final TransactionTemplate transactionTemplate;

    public IngestionService(
            PdfTextExtractor extractor,
            GeminiEmbeddingClient embeddingClient,
            ChunkRepository chunkRepository,
            DocumentRepository documents,
            DocumentProperties properties,
            PlatformTransactionManager transactionManager) {
        this.extractor = extractor;
        this.embeddingClient = embeddingClient;
        this.chunkRepository = chunkRepository;
        this.documents = documents;
        this.properties = properties;
        this.chunker = new TextChunker(properties.chunkMaxTokens(), properties.chunkOverlapTokens());
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Async("ingestionExecutor")
    public void ingest(UUID documentId, UUID userId, byte[] pdf) {
        long started = System.nanoTime();
        try {
            ExtractedPdf extracted = extractor.extract(pdf, properties.maxPages());
            List<TextChunk> chunks = chunker.chunk(extracted.pages());
            if (chunks.isEmpty()) {
                throw new IngestionException(
                        "No readable text was found in this PDF. Scanned documents are not supported yet.");
            }

            List<float[]> embeddings = embeddingClient.embedDocuments(
                    chunks.stream().map(TextChunk::content).toList());

            if (!documents.existsById(documentId)) {
                log.info("Document {} was deleted while it was processing", documentId);
                return;
            }

            // Chunks and status change together: a document is READY only with all of its chunks.
            transactionTemplate.executeWithoutResult(status -> {
                chunkRepository.insertAll(documentId, userId, chunks, embeddings);
                documents.markReady(documentId, extracted.pageCount(), chunks.size());
            });
            log.info(
                    "Document {} is ready: {} pages, {} chunks, {} ms",
                    documentId,
                    extracted.pageCount(),
                    chunks.size(),
                    (System.nanoTime() - started) / 1_000_000);
        } catch (IngestionException ex) {
            log.warn("Document {} could not be processed: {}", documentId, ex.getMessage());
            fail(documentId, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error while processing document {}", documentId, ex);
            fail(documentId, "Processing failed. Please try again.");
        }
    }

    private void fail(UUID documentId, String message) {
        String shortened = message.length() > MAX_MESSAGE_LENGTH
                ? message.substring(0, MAX_MESSAGE_LENGTH)
                : message;
        documents.markFailed(documentId, shortened);
    }
}
