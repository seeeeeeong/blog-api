Write a Flyway migration for the following schema change: $ARGUMENTS

## Rules

1. Check the latest version in `src/main/resources/db/migration/` (currently at V8)
2. Filename: `V{N}__{snake_case_description}.sql`
3. SQL guidelines:
   - Use `IF EXISTS` / `IF NOT EXISTS` for idempotency
   - Add column: `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`
   - Drop column: `ALTER TABLE ... DROP COLUMN IF EXISTS`
   - Index: `CREATE INDEX IF NOT EXISTS` or `CONCURRENTLY` for large tables
   - pg extensions: wrap in `DO $$ BEGIN ... EXCEPTION WHEN ... END $$`
4. Update the corresponding Entity class (add/remove fields)
5. `ddl-auto: validate` means Entity ↔ migration mismatch will crash the app on startup
6. Run `./gradlew test` — all tests must pass
