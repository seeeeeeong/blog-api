-- pg_trgm GIN 인덱스: ILIKE '%keyword%' 쿼리에 인덱스 지원 (부분 문자열 의미 유지)
--
-- NOTE: CREATE EXTENSION 은 superuser 권한이 필요합니다.
--   관리형 DB(RDS 등)에서는 배포 전 DBA가 먼저 실행해야 합니다:
--   CREATE EXTENSION IF NOT EXISTS pg_trgm;
--
-- 권한이 없으면 마이그레이션은 성공하되 인덱스만 건너뜁니다.
-- 이 경우 검색은 동작하지만 seq scan 으로 fallback 됩니다.

DO $$
BEGIN
    -- 1) extension 생성 시도 (superuser 필요)
    BEGIN
        CREATE EXTENSION IF NOT EXISTS pg_trgm;
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'pg_trgm extension creation skipped (%). Pre-install manually: CREATE EXTENSION pg_trgm;', SQLERRM;
    END;

    -- 2) extension + 대상 테이블이 있을 때만 인덱스 생성
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = current_schema()
              AND table_name = 'posts'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_posts_title_trgm   ON posts USING GIN (title   gin_trgm_ops);
            CREATE INDEX IF NOT EXISTS idx_posts_content_trgm ON posts USING GIN (content gin_trgm_ops);
            RAISE NOTICE 'pg_trgm indexes created.';
        ELSE
            RAISE NOTICE 'posts table not found in schema "%". Trigram indexes skipped.', current_schema();
        END IF;
    ELSE
        RAISE NOTICE 'pg_trgm not available — trigram indexes skipped. Search fallback to seq scan.';
    END IF;
END $$;
