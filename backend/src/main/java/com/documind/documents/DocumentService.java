package com.documind.documents;

import com.documind.common.ApiException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private static final HttpStatus TOO_LARGE = HttpStatus.valueOf(413);
    private static final String BUSY_MESSAGE = "The server is busy. Please try again shortly.";

    private final DocumentRepository documents;
    private final IngestionService ingestion;
    private final DocumentProperties properties;

    public DocumentService(
            DocumentRepository documents, IngestionService ingestion, DocumentProperties properties) {
        this.documents = documents;
        this.ingestion = ingestion;
        this.properties = properties;
    }

    /** Validates and stores the upload, then processes it in the background. */
    public DocumentResponse upload(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a PDF file to upload");
        }
        if (file.getSize() > properties.maxSize().toBytes()) {
            throw new ApiException(
                    TOO_LARGE, "The file is too large. The limit is " + properties.maxSize().toMegabytes() + " MB");
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF files are supported");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The file could not be read");
        }
        if (!hasPdfSignature(bytes)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The file is not a valid PDF");
        }

        Document saved = documents.save(new Document(userId, sanitizeFilename(file.getOriginalFilename()), bytes.length));
        try {
            ingestion.ingest(saved.getId(), userId, bytes);
        } catch (TaskRejectedException ex) {
            documents.markFailed(saved.getId(), BUSY_MESSAGE);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, BUSY_MESSAGE);
        }
        return DocumentResponse.from(saved);
    }

    public List<DocumentResponse> list(UUID userId) {
        return documents.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentResponse get(UUID userId, UUID id) {
        return documents.findByIdAndUserId(id, userId)
                .map(DocumentResponse::from)
                .orElseThrow(DocumentService::notFound);
    }

    public void delete(UUID userId, UUID id) {
        if (documents.deleteByIdAndUserId(id, userId) == 0) {
            throw notFound();
        }
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Document not found");
    }

    /** A real PDF starts with "%PDF-" within its first kilobyte. */
    private static boolean hasPdfSignature(byte[] bytes) {
        int last = Math.min(bytes.length - 5, 1024);
        for (int i = 0; i <= last; i++) {
            if (bytes[i] == '%' && bytes[i + 1] == 'P' && bytes[i + 2] == 'D'
                    && bytes[i + 3] == 'F' && bytes[i + 4] == '-') {
                return true;
            }
        }
        return false;
    }

    static String sanitizeFilename(String original) {
        String name = original == null ? "" : original;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("\\p{Cntrl}", "").strip();
        if (name.isEmpty()) {
            name = "document.pdf";
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }
}
