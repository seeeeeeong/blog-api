-- Populate content_vector for similarity tests without external embeddings.
-- Requires pgvector extension.
UPDATE posts
SET content_vector = (
    ARRAY(SELECT 0.01::float4 FROM generate_series(1, 1536))
)::vector
WHERE content_vector IS NULL;
