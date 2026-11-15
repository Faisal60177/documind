package com.documind.documents.ingest;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Stores chunks with their pgvector embeddings. Uses plain SQL instead of an entity mapping. */
@Repository
public class ChunkRepository {

    private static final String INSERT = """
            INSERT INTO chunks (document_id, user_id, chunk_index, page, page_end, content, embedding)
            VALUES (?, ?, ?, ?, ?, ?, ?::vector)
            """;

    private final JdbcTemplate jdbc;

    public ChunkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void insertAll(UUID documentId, UUID userId, List<TextChunk> chunks, List<float[]> embeddings) {
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("Each chunk needs exactly one embedding");
        }
        jdbc.batchUpdate(INSERT, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                TextChunk chunk = chunks.get(i);
                ps.setObject(1, documentId);
                ps.setObject(2, userId);
                ps.setInt(3, chunk.index());
                ps.setInt(4, chunk.pageStart());
                ps.setInt(5, chunk.pageEnd());
                ps.setString(6, chunk.content());
                ps.setString(7, toVectorLiteral(embeddings.get(i)));
            }

            @Override
            public int getBatchSize() {
                return chunks.size();
            }
        });
    }

    /** Formats a vector the way pgvector expects it, for example {@code [0.1,0.2,0.3]}. */
    static String toVectorLiteral(float[] vector) {
        StringBuilder literal = new StringBuilder(vector.length * 10).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (!Float.isFinite(vector[i])) {
                throw new IllegalArgumentException("Embedding contains a non-finite value");
            }
            if (i > 0) {
                literal.append(',');
            }
            literal.append(vector[i]);
        }
        return literal.append(']').toString();
    }
}
