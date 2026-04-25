# blog-api

Personal blog API built with Kotlin + Spring Boot 3 + JPA + PostgreSQL.
Serves public blog reads and admin post/image management.

## Tech Stack

- Kotlin 2.3, Spring Boot 3.5, Spring Security (JWT stateless)
- PostgreSQL + Flyway migrations
- Caffeine cache, AWS S3 (presigned URL)
- Gradle (ktlint, detekt, ArchUnit)

---

## Package Structure (Non-Negotiable)

```
com.blog.api
├── core
│   ├── api
│   │   ├── config          # Security, Cache, Web, S3 configs
│   │   └── controller/v1
│   │       ├── request      # Request DTOs (data class, validation)
│   │       └── response     # Response DTOs (companion of())
│   ├── domain
│   │   └── {context}        # Service, Reader, Command, Domain model, Event
│   ├── enum                 # Shared enums (PostStatus, UserRole)
│   └── support
│       ├── auth             # @Admin, @ResolveCurrentUser resolvers
│       ├── converter        # Markdown → HTML
│       ├── error            # CoreException, ErrorType
│       ├── nickname         # RandomNicknameGenerator
│       ├── properties       # @ConfigurationProperties
│       ├── response         # ApiResponse<T>, PageResponse
│       ├── security         # JWT provider, filter
│       └── web              # MdcLoggingFilter, RateLimitFilter
└── storage
    └── {context}            # Entity, Repository, Extensions
```

**Never do:**
- Add top-level packages outside `core` and `storage`
- Depend on `controller` from `storage`
- Access `storage` directly from `controller` (always go through `domain` services)
- Expose JPA entities in controller responses

---

## Coding Conventions

### Naming

| Target | Rule | Example |
|--------|------|---------|
| Command object | `{Entity}{Action}` | `PostCreate`, `CommentCreate` |
| Service method | verb + object | `createPost`, `deleteCommentByAdmin` |
| Reader (read-only) | `get/search/render` | `getPost`, `searchPosts`, `renderHtml` |
| Request DTO | `{Entity}{Action}Request` | `PostCreateRequest` |
| Response DTO | `{Entity}Response` | `PostResponse`, `CommentResponse` |
| Repository query | Spring Data naming or `@Query` | `findByPostId`, `existsByIdAndStatus` |
| Event | `{Entity}{PastTense}Event` | `PostCacheEvictEvent`, `PostDeletedEvent` |
| Validation private method | `require{Condition}` | `requireOwner`, `requirePublishedPost` |

### Style Rules

- Always use trailing commas
- Never use `!!` — use `?:`, `?.let`, or `requireNotNull` instead
- Extract a Command object when function parameters exceed 4
- Never branch on Boolean parameters — split into separate functions
- Keep branching flat with guard clauses (max 2 levels of nesting)
- Functions must stay under 40 lines
- Log in English using KotlinLogging
- Split files by *public coupling*, not by "one type per file." A type lives in its own file when it is part of a **public contract** — Request/Response DTOs (Controller↔Service boundary), domain models, types accepted from or returned to another bounded context, Commands consumed by multiple services, Events (cross-service via Spring's event bus), Hits/Results visible across layers. **Internal implementation data types** of a single feature flow — Snapshots, intermediate aggregates, Commands consumed only by helpers of one service — go in a sibling **`{Feature}Internals.kt`** file next to the owner service/reader in the same package. **Helper services / Spring components** always live in their own `.kt` file even when they're scoped to one flow — never colocate `@Service` / `@Component` classes with data classes in `Internals.kt`, and never put data classes inside the same file as a service class. The aim is "follow one feature in two or three files (use-case service + Internals.kt + helper service if any)," not "one type per file regardless." If a type acquires a caller outside its origin feature, promote it to its own file.

```kotlin
// Good — service files hold only services; Internals.kt holds only data classes
// PostService.kt                  → use-case service only
class PostService(...) { ... }

// PostInternals.kt                → sibling file, data classes only
internal data class PostUpdateContext(...)        // single-flow scratch
internal data class PostSearchProjection(...)     // helper aggregate

// Public Request/Response/domain model/Event — still own file
// PostCreateRequest.kt, PostResponse.kt, Post.kt, PostDeletedEvent.kt
```

  Prefer `internal` visibility for co-located data types — it documents the single-owner intent and stops cross-package callers from forming. Treat it as a recommendation, not a hard rule: Spring DI (a `public` service cannot accept an `internal` constructor parameter — Kotlin's `EXPOSED_PARAMETER_TYPE`), public method signatures that include the type, Jackson serialization, or test access may legitimately require broader visibility. Drop `internal` when one of those constraints actually bites; don't add it just to satisfy a checklist.
- Keep `if` conditions plain. Avoid `!`, avoid safe-call chains (`x?.foo() == true`), avoid null comparisons buried in compound conditions. Flatten the value first — with `?: return`, `?: continue`, `takeIf { ... }`, or a named boolean — so the `if` itself reads as a domain concept on a non-nullable receiver. Prefer a positive condition with early return, then handle the failure case after; the happy path reads forward instead of as "not the bad thing." Prefer the positive form of negated extension calls (`isNotBlank()` over `!isNullOrBlank()`, `isNotEmpty()` over `!isEmpty()`).

```kotlin
// Bad — negation buried in a method call
if (!postRepository.existsById(postId)) {
    throw CoreException(ErrorType.POST_NOT_FOUND)
}

// Good — name the boolean, branch positively, throw after
val postExists = postRepository.existsById(postId)
if (postExists) return
throw CoreException(ErrorType.POST_NOT_FOUND)

// Bad — negated extension call, or safe-call chain in the condition
if (!nickname.isNullOrBlank()) return nickname
if (nickname?.isNotBlank() == true) return nickname

// Good — flatten with `?: return`, then a plain positive check
val nickname = request.nickname ?: return defaultNickname()
if (nickname.isNotBlank()) return nickname
```

- Use `?.` (safe-call) sparingly — only when each step in the chain has a genuinely nullable receiver from an external boundary you don't control. Don't paper over branching logic with long `?.foo()?.bar()?.baz()` chains; flatten to non-null at the earliest point with `?: return` / `?: continue`, then operate on the non-nullable value. Multiple `?.` calls in a row are a code smell — usually one of the receivers is non-nullable in practice and the chain is hiding a clearer guard. Two `?.` calls walking a third-party object graph are fine — that's the "necessary moment."

```kotlin
// Bad — long safe-call chain hiding the control flow
val nickname = request.profile
    ?.displayName
    ?.trim()
    ?.takeIf { it.isNotBlank() }
return nickname ?: defaultNickname()

// Good — bail out early via elvis, then work on a non-null String
val profile = request.profile ?: return defaultNickname()
val nickname = profile.displayName?.trim().orEmpty()
if (nickname.isNotBlank()) return nickname
return defaultNickname()
```

### API Response

All responses are wrapped in `ApiResponse<T>`:

```kotlin
// Success
ApiResponse.success(data)
ApiResponse.success() // for 204 or no-body responses

// Errors — throw CoreException, ApiControllerAdvice handles conversion
throw CoreException(ErrorType.POST_NOT_FOUND)
```

### Request DTO Pattern

```kotlin
data class PostCreateRequest(
    val categoryId: Long,
    @field:NotBlank @field:Size(max = 200) val title: String,
    @field:NotBlank @field:Size(max = 50000) val content: String,
) {
    fun toCommand(userId: Long) = PostCreate(
        userId = userId,
        categoryId = categoryId,
        title = title,
        content = content,
    )
}
```

- Validation annotations require `@field:` prefix
- Convert to domain command via `toCommand()` method
- Use default parameters only for fields that genuinely have defaults

### Response DTO Pattern

```kotlin
data class CommentResponse(
    val id: Long,
    val nickname: String,
    val contentHtml: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(comment: Comment): CommentResponse { ... }
    }
}
```

- Use `companion object { fun of() }` factory method
- Map from domain objects only — never reference entities directly

---

## Entity & Storage Rules

### Entity

```kotlin
@Entity
@Table(name = "posts")
class PostEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val userId: Long,
    content: String,
) : BaseTimeEntity() {
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String = content
        protected set

    fun updateContent(newContent: String) { this.content = newContent }
}
```

- Extend `BaseTimeEntity` (auto-managed `createdAt`, `updatedAt`)
- Mutable fields: constructor param → body `var ... protected set`
- Encapsulate state changes behind methods (`softDelete()`, `restore()`, `updateContent()`)

### Extension Functions

Entity → Domain conversion lives in `{Entity}Extensions.kt`:

```kotlin
fun PostEntity.toPost(): Post = Post(
    id = requireNotNull(id),
    ...
)
```

### Soft Delete

- Use `@SQLDelete` + `@SQLRestriction("is_deleted = false")` or status enum (`DELETED`)
- Never physically delete rows

---

## Database Migration (Flyway)

- Filename: `V{N}__{snake_case_description}.sql`
- Currently at V8 — next migration starts at V9
- Use `IF EXISTS` / `IF NOT EXISTS` for idempotency
- Use `CONCURRENTLY` for indexes on large tables
- Wrap pg extensions in `DO $$ BEGIN ... EXCEPTION WHEN ... END $$` for graceful skip

---

## Security Patterns

### Authentication

- JWT stateless (access 1h, refresh 14d)
- `JwtAuthenticationFilter` sets principal=userId in `SecurityContextHolder`
- Comment paths (`/api/v1/posts/{id}/comments`) bypass JWT filter

### Authorization

- `@Admin userId: Long` — AdminArgumentResolver verifies ROLE_ADMIN + extracts userId
- `@ResolveCurrentUser currentUser: CurrentUser` — allows unauthenticated users (nullable userId)
- SecurityConfig specifies `hasRole` / `permitAll` per endpoint

### Rate Limiting

- `RateLimitFilter`: login 10 req/min, comments 5 req/min (IP-based, Caffeine)

---

## Error Handling

```kotlin
// Define: ErrorType enum
POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_001", "Post not found", LogLevel.WARN)

// Throw: CoreException
throw CoreException(ErrorType.POST_NOT_FOUND)

// Catch: ApiControllerAdvice auto-converts to ApiResponse.error()
```

- Add new errors to the `ErrorType` enum
- Code format: `{DOMAIN}_{number}` (USER_001, POST_001, AUTH_001, COMMON_001)
- logLevel: client fault (4xx) → INFO/WARN, server fault (5xx) → ERROR

---

## Event Pattern

Use Spring Events for async / post-commit work:

```kotlin
// Publish (inside @Transactional Service)
eventPublisher.publishEvent(PostCacheEvictEvent(postId))

// Listen (separate @Component)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun onPostChanged(event: PostCacheEvictEvent) { ... }
```

- Default to `AFTER_COMMIT`
- Add `@Async` for external I/O (e.g., S3 deletion)

---

## Testing

### Commands

```bash
./gradlew test          # all tests
./gradlew ktlintCheck   # format check
./gradlew detekt        # static analysis
```

### Structure

- Unit tests: Mockito mocks with `fixture()` helper pattern
- Integration tests: `@DataJpaTest` + TestContainers PostgreSQL
- Architecture tests: ArchUnit for layer dependency enforcement

### Fixture Pattern

```kotlin
private fun fixture(
    postRepository: PostRepository = mock(),
    eventPublisher: ApplicationEventPublisher = mock(),
) = PostService(postRepository = postRepository, eventPublisher = eventPublisher)
```

---

## Verification Checklist

After any code change:

1. `./gradlew test` — all tests pass
2. New feature or bugfix → add tests
3. Schema change → add Flyway migration
4. New error type → add to `ErrorType` enum
5. New endpoint → add authorization rule in `SecurityConfig`

---

## What NOT To Do

- Return entities directly as responses
- Call repositories from controllers
- Write operations without `@Transactional`
- Physical deletion (always soft delete)
- Log passwords, tokens, or secrets
- Declare work complete without running tests
- Introduce new patterns without discussion
- Swallow errors with `runCatching { }.getOrDefault()`
