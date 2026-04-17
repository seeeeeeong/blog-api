Add the following API endpoint: $ARGUMENTS

## Implementation Order

1. Analyze existing similar endpoints (Controller, SecurityConfig)
2. Request DTO:
   - `@field:NotBlank`, `@field:Size` validation
   - `toCommand()` method for domain conversion
3. Response DTO:
   - `companion object { fun of(domain): Response }` factory
4. Controller method:
   - Appropriate HTTP method annotation (`@PostMapping`, `@GetMapping`, etc.)
   - `@ResponseStatus` where needed (201 CREATED, 204 NO_CONTENT)
   - Wrap with `ApiResponse.success()`
   - Auth: `@Admin userId: Long` or `@ResolveCurrentUser currentUser: CurrentUser`
5. Add authorization rule in SecurityConfig:
   - `.hasRole("ADMIN")` or `.permitAll()`
   - Check if JwtAuthenticationFilter bypass is needed
6. Implement service logic
7. Add Flyway migration if schema changes are needed
8. Run `./gradlew test` — all tests must pass
