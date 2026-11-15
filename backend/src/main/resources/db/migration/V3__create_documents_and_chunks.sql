CREATE TABLE documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    filename      VARCHAR(255) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    page_count    INTEGER,
    chunk_count   INTEGER      NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL,
    error_message VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_documents_status CHECK (status IN ('PROCESSING', 'READY', 'FAILED'))
);

CREATE INDEX idx_documents_user_created ON documents (user_id, created_at DESC);

CREATE TABLE chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID        NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    chunk_index INTEGER     NOT NULL,
    page        INTEGER     NOT NULL,
    page_end    INTEGER     NOT NULL,
    content     TEXT        NOT NULL,
    embedding   vector(768) NOT NULL,
    CONSTRAINT uq_chunks_document_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_chunks_document_id ON chunks (document_id);
CREATE INDEX idx_chunks_user_id ON chunks (user_id);
CREATE INDEX idx_chunks_embedding ON chunks USING hnsw (embedding vector_cosine_ops);
