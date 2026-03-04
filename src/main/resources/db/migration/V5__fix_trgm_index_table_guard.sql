-- V2에서 누락된 posts 테이블 존재 여부 guard 추가
-- pg_trgm 인덱스가 posts 테이블 없이 실행될 경우를 대비
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE schemaname = current_schema()
              AND tablename  = 'posts'
              AND indexname  = 'idx_posts_title_trgm'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_posts_title_trgm   ON posts USING GIN (title   gin_trgm_ops);
            CREATE INDEX IF NOT EXISTS idx_posts_content_trgm ON posts USING GIN (content gin_trgm_ops);
            RAISE NOTICE 'pg_trgm indexes (re)created by V5.';
        END IF;
    END IF;
END $$;
