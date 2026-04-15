-- Migrate comments table from OAuth-based to anonymous (nickname + password) model.
-- Existing rows get nickname copied from oauth_username, password set to a placeholder.

ALTER TABLE comments ADD COLUMN IF NOT EXISTS nickname VARCHAR(50);
ALTER TABLE comments ADD COLUMN IF NOT EXISTS password VARCHAR(255);

UPDATE comments SET nickname = COALESCE(oauth_username, 'unknown') WHERE nickname IS NULL;
UPDATE comments SET password = '' WHERE password IS NULL;

ALTER TABLE comments ALTER COLUMN nickname SET NOT NULL;
ALTER TABLE comments ALTER COLUMN password SET NOT NULL;

-- Drop legacy OAuth columns
ALTER TABLE comments DROP COLUMN IF EXISTS oauth_id;
ALTER TABLE comments DROP COLUMN IF EXISTS oauth_username;
ALTER TABLE comments DROP COLUMN IF EXISTS oauth_avatar_url;
