-- Reset data for local load testing.
-- WARNING: This is destructive. Use only on local/dev.
TRUNCATE TABLE comments, posts, categories, users RESTART IDENTITY CASCADE;
