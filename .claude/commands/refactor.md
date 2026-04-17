Refactor the following: $ARGUMENTS

## Principles

1. **Improve structure without changing behavior** — API spec must remain identical
2. All existing tests must pass
3. Never mix large renames with logic changes in the same step

## Order

1. Analyze target code — understand current structure and dependencies
2. Plan the refactoring (which files change and how)
3. Apply changes incrementally:
   - Improve naming (names should reveal intent)
   - Flatten nesting with guard clauses
   - Extract Command objects for 4+ parameters
   - Extract duplicated logic into private methods (avoid premature abstraction)
4. Run `./gradlew test` — all tests must pass

## Checklist

- [ ] Follows clean-code.md rules
- [ ] Trailing commas preserved
- [ ] Entity → Domain → Response layer separation maintained
- [ ] ArchUnit tests pass (layer dependency enforcement)
