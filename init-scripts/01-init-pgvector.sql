-- pgvector 확장 설치

-- pgvector 확장 생성
CREATE EXTENSION IF NOT EXISTS vector;

DO $$
BEGIN
    RAISE NOTICE 'pgvector extension installed successfully';
END $$;
