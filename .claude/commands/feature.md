Implement the following feature: $ARGUMENTS

## Implementation Order

1. Analyze existing code — identify related domain and similar features
2. Domain layer first:
   - Command object in `core/domain/{context}/` (`{Entity}Create`, etc.)
   - Domain model (data class)
   - Service with `@Transactional`, validation via `require{Condition}` private methods
3. Storage layer:
   - Entity in `storage/{context}/` (extend `BaseTimeEntity`, `protected set`)
   - Repository (JpaRepository, `@Query` if needed)
   - Extensions (`toEntity()`, `toDomain()`)
4. API layer:
   - Request DTO with `toCommand()` method, `@field:` validation
   - Response DTO with `companion object { fun of() }` pattern
   - Controller returning `ApiResponse.success()`
5. Security:
   - Add endpoint authorization in SecurityConfig
   - Use `@Admin userId: Long` for authenticated endpoints
6. Add error type to `ErrorType` enum if needed
7. Add Flyway migration for schema changes (currently at V8)
8. Write tests (unit: Mockito fixture pattern, integration: @DataJpaTest)
9. Run `./gradlew test` — all tests must pass

## Must Follow

- All conventions in CLAUDE.md
- Trailing commas
- No `!!` operator
- All API responses wrapped in `ApiResponse<T>`
- Errors thrown as `CoreException(ErrorType.XXX)`
- Never expose entities to controllers
- Consistent naming and style with existing code
