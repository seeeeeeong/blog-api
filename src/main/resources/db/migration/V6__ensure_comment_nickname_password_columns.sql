-- Idempotent re-application of V4 columns for databases where V4 was
-- recorded as applied but the DDL did not actually execute.

ALTER TABLE comments ADD COLUMN IF NOT EXISTS nickname VARCHAR(50);
ALTER TABLE comments ADD COLUMN IF NOT EXISTS password VARCHAR(255);

UPDATE comments SET nickname = 'unknown' WHERE nickname IS NULL;
UPDATE comments SET password = '' WHERE password IS NULL;

ALTER TABLE comments ALTER COLUMN nickname SET NOT NULL;
ALTER TABLE comments ALTER COLUMN password SET NOT NULL;
