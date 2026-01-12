-- Seed data for local load testing.
-- Assumes a clean database or running after reset.sql for deterministic IDs.

INSERT INTO categories (name, slug, created_at, updated_at)
SELECT
    CONCAT('Category ', g),
    CONCAT('category-', g),
    NOW(),
    NOW()
FROM generate_series(1, 5) AS g
ON CONFLICT DO NOTHING;

INSERT INTO users (email, password, nickname, role, created_at, updated_at)
SELECT
    CONCAT('user', g, '@example.com'),
    '$2b$10$abcdefghijklmnopqrstuv', -- fake bcrypt
    CONCAT('User', g),
    'USER',
    NOW(),
    NOW()
FROM generate_series(1, 50) AS g
ON CONFLICT DO NOTHING;

INSERT INTO posts (user_id, category_id, title, content, status, view_count, created_at, updated_at)
SELECT
    ((g - 1) % 50) + 1,
    ((g - 1) % 5) + 1,
    CONCAT('Post ', g),
    repeat('Sample content ', 50),
    'PUBLISHED',
    0,
    NOW() - (g || ' minutes')::interval,
    NOW() - (g || ' minutes')::interval
FROM generate_series(1, 2000) AS g;
