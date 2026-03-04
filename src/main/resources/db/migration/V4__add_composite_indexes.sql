-- posts 복합 인덱스: user+status, category+status 필터 쿼리 최적화
CREATE INDEX IF NOT EXISTS idx_post_user_status      ON posts (user_id, status);
CREATE INDEX IF NOT EXISTS idx_post_category_status  ON posts (category_id, status);
