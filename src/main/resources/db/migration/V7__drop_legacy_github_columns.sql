-- Drop legacy GitHub OAuth columns that V4 missed because the production
-- schema used "github_*" names instead of "oauth_*".

ALTER TABLE comments DROP COLUMN IF EXISTS github_id;
ALTER TABLE comments DROP COLUMN IF EXISTS github_username;
ALTER TABLE comments DROP COLUMN IF EXISTS github_avatar_url;
