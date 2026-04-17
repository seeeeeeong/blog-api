-- Remove reply structure and password-based edit/delete
-- Comments are now anonymous with random nicknames, no edit/delete by users

ALTER TABLE comments DROP COLUMN IF EXISTS parent_id;
ALTER TABLE comments DROP COLUMN IF EXISTS password;
