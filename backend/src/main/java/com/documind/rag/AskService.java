package com.documind.rag;

import com.documind.common.ApiException;
import com.documind.documents.ingest.GeminiEmbeddingClient;
import com.documind.documents.ingest.IngestionException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AskService {

    private static final int FALLBACK_SOURCES = 3;
    private static final String DEFAULT_NOT_FOUND = "Your documents do not contain an answer to this question.";

    private final GeminiEmbeddingClient embeddingClient;
    private final ChunkSearchRepository chunkSearch;
    private final GeminiGenerationClient generator;
    private final RagProperties properties;

    public AskService(
            GeminiEmbeddingClient embeddingClient,
            ChunkSearchRepository chunkSearch,
            GeminiGenerationClient generator,
            RagProperties properties) {
        this.embeddingClient = embeddingClient;
        this.chunkSearch = chunkSearch;
        this.generator = generator;
        this.properties = properties;
    }

    public AskResponse ask(UUID userId, String question) {
        String trimmed = question.strip();

        float[] queryVector;
        try {
            queryVector = embeddingClient.embedQuery(trimmed);
        } catch (IngestionException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        }

        List<RetrievedChunk> chunks = chunkSearch.search(userId, queryVector, properties.topK());
        if (chunks.isEmpty()) {
            return new AskResponse(AnswerStatus.NO_DOCUMENTS, null, List.of());
        }

        String raw = generator.generate(PromptBuilder.SYSTEM_INSTRUCTION, PromptBuilder.userPrompt(trimmed, chunks));
        ParsedAnswer parsed = AnswerParser.parse(raw, chunks.size());

        if (!parsed.found()) {
            String message = parsed.text().isBlank() ? DEFAULT_NOT_FOUND : parsed.text();
            return new AskResponse(AnswerStatus.NOT_FOUND, message, List.of());
        }
        if (parsed.text().isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI model returned an empty answer. Please try again.");
        }

        // If the model forgot to cite, show the best passages it was given instead of nothing.
        List<Integer> cited = parsed.citedContextNumbers().isEmpty()
                ? IntStream.rangeClosed(1, Math.min(FALLBACK_SOURCES, chunks.size())).boxed().toList()
                : parsed.citedContextNumbers();

        List<SourceResponse> sources = new ArrayList<>();
        for (int i = 0; i < cited.size(); i++) {
            sources.add(toSource(i + 1, chunks.get(cited.get(i) - 1)));
        }
        return new AskResponse(AnswerStatus.ANSWERED, parsed.text(), sources);
    }

    private SourceResponse toSource(int number, RetrievedChunk chunk) {
        return new SourceResponse(
                number,
                chunk.documentId(),
                chunk.filename(),
                chunk.page(),
                chunk.pageEnd(),
                snippet(chunk.content()),
                Math.round(chunk.score() * 1000.0) / 1000.0);
    }

    private String snippet(String content) {
        int limit = properties.snippetLength();
        if (content.length() <= limit) {
            return content;
        }
        int end = Character.isHighSurrogate(content.charAt(limit - 1)) ? limit - 1 : limit;
        return content.substring(0, end).strip() + "…";
    }
}
