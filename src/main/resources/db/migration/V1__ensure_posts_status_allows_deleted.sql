DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'posts'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_schema = current_schema()
              AND table_name = 'posts'
              AND constraint_name = 'posts_status_check'
              AND constraint_type = 'CHECK'
        ) THEN
            ALTER TABLE posts DROP CONSTRAINT posts_status_check;
        END IF;

        ALTER TABLE posts
            ADD CONSTRAINT posts_status_check
                CHECK (status IN ('DRAFT', 'PUBLISHED', 'DELETED'));
    END IF;
END $$;
