package com.documind.rag;

import com.documind.common.ApiException;
import com.documind.documents.ingest.GeminiProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Generates text with a Gemini Flash model through the generateContent API. */
@Component
public class GeminiGenerationClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiGenerationClient.class);

    private final GeminiProperties gemini;
    private final RagProperties properties;
    private final RestClient restClient;

    public GeminiGenerationClient(GeminiProperties gemini, RagProperties properties) {
        this.gemini = gemini;
        this.properties = properties;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.generationTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(gemini.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public String generate(String systemInstruction, String prompt) {
        if (gemini.apiKey() == null || gemini.apiKey().isBlank()) {
            log.error("GEMINI_API_KEY is not configured");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Answers are not available right now. Please try again later.");
        }

        ThinkingConfig thinking = properties.thinkingLevel() == null || properties.thinkingLevel().isBlank()
                ? null
                : new ThinkingConfig(properties.thinkingLevel().trim());
        GenerateRequest request = new GenerateRequest(
                new Content(null, List.of(new Part(systemInstruction))),
                List.of(new Content("user", List.of(new Part(prompt)))),
                new GenerationConfig(properties.maxOutputTokens(), thinking));

        Duration backoff = properties.generationBackoff();
        for (int attempt = 0; ; attempt++) {
            try {
                GenerateResponse response = restClient.post()
                        .uri("/models/{model}:generateContent", properties.generationModel())
                        .header("x-goog-api-key", gemini.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(GenerateResponse.class);
                return extractText(response);
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                log.warn("Gemini generation failed with HTTP {} (attempt {}): {}",
                        status, attempt + 1, abbreviate(ex.getResponseBodyAsString()));
                if (!retryable || attempt >= properties.generationMaxRetries()) {
                    throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, messageFor(status));
                }
                backoff = pauseAndDouble(backoff);
            } catch (ResourceAccessException ex) {
                log.warn("Gemini generation failed: {} (attempt {})", ex.getMessage(), attempt + 1);
                if (attempt >= properties.generationMaxRetries()) {
                    throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                            "The AI service could not be reached. Please try again later.");
                }
                backoff = pauseAndDouble(backoff);
            }
        }
    }

    private String extractText(GenerateResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            log.warn("Gemini returned no candidates (the prompt may have been blocked)");
            throw emptyAnswer();
        }
        Candidate candidate = response.candidates().getFirst();
        if (candidate.content() == null || candidate.content().parts() == null) {
            log.warn("Gemini returned no content (finishReason={})", candidate.finishReason());
            throw emptyAnswer();
        }
        String text = candidate.content().parts().stream()
                .filter(part -> part.text() != null && !Boolean.TRUE.equals(part.thought()))
                .map(ResponsePart::text)
                .collect(Collectors.joining());
        if ("MAX_TOKENS".equals(candidate.finishReason())) {
            log.warn("Gemini stopped at the token limit; consider raising app.rag.max-output-tokens");
        }
        if (text.isBlank()) {
            throw emptyAnswer();
        }
        return text;
    }

    private static ApiException emptyAnswer() {
        return new ApiException(HttpStatus.BAD_GATEWAY,
                "The AI model could not produce an answer for this question. Try rephrasing it.");
    }

    private static String messageFor(int status) {
        if (status == 429) {
            return "The AI service is busy (rate limit reached). Please try again in a minute.";
        }
        if (status >= 400 && status < 500) {
            return "The AI service rejected the request. Please contact the administrator.";
        }
        return "The AI service is temporarily unavailable. Please try again later.";
    }

    private static Duration pauseAndDouble(Duration backoff) {
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The request was interrupted. Please try again.");
        }
        return backoff.multipliedBy(2);
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Part(String text) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Content(String role, List<Part> parts) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ThinkingConfig(String thinkingLevel) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerationConfig(int maxOutputTokens, ThinkingConfig thinkingConfig) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerateRequest(Content systemInstruction, List<Content> contents, GenerationConfig generationConfig) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResponsePart(String text, Boolean thought) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResponseContent(List<ResponsePart> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(ResponseContent content, String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates) {}
}
