package com.documind.documents.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Creates 768-dimension embeddings with the Gemini API. Vectors are L2-normalized. */
@Component
public class GeminiEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(90);

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiEmbeddingClient(GeminiProperties properties) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.requestTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /** Embeds passages that will be stored and searched later. */
    public List<float[]> embedDocuments(List<String> texts) {
        return embed(texts, "RETRIEVAL_DOCUMENT");
    }

    /** Embeds a user question. Used for retrieval in the next phase. */
    public float[] embedQuery(String text) {
        return embed(List.of(text), "RETRIEVAL_QUERY").getFirst();
    }

    private List<float[]> embed(List<String> texts, String taskType) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.error("GEMINI_API_KEY is not configured");
            throw new IngestionException("Document processing is not available right now. Please try again later.");
        }
        List<float[]> vectors = new ArrayList<>(texts.size());
        int batchSize = properties.batchSize();
        for (int from = 0; from < texts.size(); from += batchSize) {
            if (from > 0) {
                pause(properties.batchDelay());
            }
            List<String> batch = texts.subList(from, Math.min(from + batchSize, texts.size()));
            vectors.addAll(embedBatch(batch, taskType));
        }
        return vectors;
    }

    private List<float[]> embedBatch(List<String> batch, String taskType) {
        String modelName = "models/" + properties.embeddingModel();
        BatchRequest request = new BatchRequest(batch.stream()
                .map(text -> new EmbedRequest(
                        modelName,
                        new Content(List.of(new Part(text))),
                        taskType,
                        properties.embeddingDimensions()))
                .toList());

        Duration backoff = properties.initialBackoff();
        for (int attempt = 0; ; attempt++) {
            try {
                BatchResponse response = restClient.post()
                        .uri("/models/{model}:batchEmbedContents", properties.embeddingModel())
                        .header("x-goog-api-key", properties.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(BatchResponse.class);
                return toVectors(response, batch.size());
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                log.warn("Gemini embeddings request failed with HTTP {} (attempt {})", status, attempt + 1);
                if (!retryable || attempt >= properties.maxRetries()) {
                    throw new IngestionException(messageFor(status), ex);
                }
                backoff = waitBeforeRetry(backoff, ex.getResponseHeaders());
            } catch (ResourceAccessException ex) {
                log.warn("Gemini embeddings request failed: {} (attempt {})", ex.getMessage(), attempt + 1);
                if (attempt >= properties.maxRetries()) {
                    throw new IngestionException(
                            "The embedding service could not be reached. Please try again later.", ex);
                }
                backoff = waitBeforeRetry(backoff, null);
            }
        }
    }

    private List<float[]> toVectors(BatchResponse response, int expected) {
        List<ContentEmbedding> embeddings = response == null ? null : response.embeddings();
        if (embeddings == null || embeddings.size() != expected) {
            throw new IngestionException("The embedding service returned an unexpected response.");
        }
        List<float[]> vectors = new ArrayList<>(expected);
        for (ContentEmbedding embedding : embeddings) {
            vectors.add(normalize(embedding.values()));
        }
        return vectors;
    }

    private float[] normalize(List<Float> values) {
        if (values == null || values.size() != properties.embeddingDimensions()) {
            throw new IngestionException("The embedding service returned an unexpected response.");
        }
        double sum = 0;
        for (float value : values) {
            sum += (double) value * value;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            throw new IngestionException("The embedding service returned an unexpected response.");
        }
        float[] normalized = new float[values.size()];
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] = (float) (values.get(i) / norm);
        }
        return normalized;
    }

    private Duration waitBeforeRetry(Duration backoff, HttpHeaders headers) {
        Duration wait = backoff;
        if (headers != null) {
            String retryAfter = headers.getFirst("Retry-After");
            if (retryAfter != null) {
                try {
                    Duration requested = Duration.ofSeconds(Long.parseLong(retryAfter.trim()));
                    if (requested.compareTo(wait) > 0) {
                        wait = requested;
                    }
                } catch (NumberFormatException ignored) {
                    // keep the exponential backoff
                }
            }
        }
        pause(wait);
        Duration next = backoff.multipliedBy(2);
        return next.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : next;
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IngestionException("Processing was interrupted. Please upload the file again.", ex);
        }
    }

    private static String messageFor(int status) {
        if (status == 429) {
            return "The embedding service is busy (rate limit reached). Please try again in a few minutes.";
        }
        if (status >= 400 && status < 500) {
            return "The embedding service rejected the request. Please contact the administrator.";
        }
        return "The embedding service is temporarily unavailable. Please try again later.";
    }

    record Part(String text) {}

    record Content(List<Part> parts) {}

    record EmbedRequest(String model, Content content, String taskType, int outputDimensionality) {}

    record BatchRequest(List<EmbedRequest> requests) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentEmbedding(List<Float> values) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BatchResponse(List<ContentEmbedding> embeddings) {}
}
