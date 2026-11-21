package com.documind.rag;

import com.documind.documents.ingest.ChunkRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ChunkSearchRepository {

    private static final String SEARCH = """
            SELECT c.id, c.document_id, d.filename, c.page, c.page_end, c.content,
                   1 - (c.embedding <=> ?::vector) AS score
              FROM chunks c
              JOIN documents d ON d.id = c.document_id
             WHERE c.user_id = ?
             ORDER BY c.embedding <=> ?::vector
             LIMIT ?
            """;

    private final JdbcTemplate jdbc;

    public ChunkSearchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Finds the passages closest to the question among this user's documents only.
     * Requires pgvector 0.8 or newer: iterative scans keep the user filter from
     * shrinking the result when the index returns other users' rows first.
     */
    @Transactional(readOnly = true)
    public List<RetrievedChunk> search(UUID userId, float[] queryEmbedding, int limit) {
        jdbc.execute("SET LOCAL hnsw.iterative_scan = strict_order");
        String vector = ChunkRepository.toVectorLiteral(queryEmbedding);
        return jdbc.query(
                SEARCH,
                (rs, rowNum) -> new RetrievedChunk(
                        rs.getObject("id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getString("filename"),
                        rs.getInt("page"),
                        rs.getInt("page_end"),
                        rs.getString("content"),
                        rs.getDouble("score")),
                vector,
                userId,
                vector,
                limit);
    }
}
