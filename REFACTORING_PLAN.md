# 📋 Blog API 리팩토링 계획 및 답변

## 질문 답변 및 개선 계획

---

## 1️⃣ RefreshToken 관련 - Filter vs 현재 구조

### 현재 구조 분석
**현재 방식**: POST /api/users/refresh 엔드포인트로 처리
```kotlin
@PostMapping("/refresh")
fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest) =
    ApiResponse.success(userService.refreshAccessToken(request.refreshToken))
```

### 답변: 현재 구조가 더 적합합니다

**이유:**

1. **명확한 책임 분리**
   - Filter: 모든 요청에 대한 JWT 검증 (Access Token)
   - Endpoint: 명시적인 토큰 갱신 요청 (Refresh Token)

2. **보안성**
   - Refresh Token은 민감 정보로 특별한 검증 필요
   - Redis 조회 + 사용자 검증 + 새 토큰 쌍 발급
   - Filter에서 이런 복잡한 로직은 부적합

3. **RESTful 설계**
   - 토큰 갱신은 "리소스 생성" 작업
   - POST 메서드로 명시적 요청이 더 적합

**Filter/Interceptor를 쓰면 안 되는 경우:**
```kotlin
// ❌ 나쁜 예: Filter에서 Refresh Token 처리
class RefreshTokenFilter : OncePerRequestFilter() {
    override fun doFilterInternal(...) {
        // 모든 요청마다 Refresh Token 체크? 비효율적
        // Redis 조회 + 새 토큰 발급? Filter 책임 과다
    }
}
```

**결론: 현재 구조 유지 권장**

---

## 2️⃣ Logging 전략

### 현재 상태 분석
```kotlin
private val logger = LoggerFactory.getLogger(PostService::class.java)

logger.debug("View count increased: postId={}, clientIp={}", postId, clientIp)
logger.error("Redis connection failed", e)
```

### 답변: 기본적으로 잘 적용되어 있으나 개선 필요

**현재 장점:**
- SLF4J + Logback 사용 ✓
- 로그 레벨 구분 (debug, error, warn) ✓
- 파라미터 바인딩 방식 사용 ✓

**개선 필요 사항:**

#### 1. 구조화된 로깅 (Structured Logging)
```kotlin
// Before
logger.info("User login: userId={}, email={}", userId, email)

// After (MDC 활용)
MDC.put("userId", userId.toString())
MDC.put("email", email)
logger.info("User login successful")
MDC.clear()
```

#### 2. AOP 기반 요청/응답 로깅
```kotlin
@Aspect
@Component
class LoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    fun logAround(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

        logger.info(
            "REQUEST: {} {} - Args: {}",
            request.method,
            request.requestURI,
            joinPoint.args
        )

        val result = joinPoint.proceed()

        logger.info(
            "RESPONSE: {} {} - Time: {}ms",
            request.method,
            request.requestURI,
            System.currentTimeMillis() - startTime
        )

        return result
    }
}
```

#### 3. 로그 레벨 가이드라인
```yaml
# application.yml
logging:
  level:
    root: INFO
    com.blog.api: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-history: 30
    max-size: 10MB
```

**개선 계획:**
1. MDC를 활용한 요청 추적 ID 추가
2. AOP 기반 공통 로깅 적용
3. 에러 로그에 Stack Trace 레벨 구분
4. 프로덕션/개발 환경별 로그 레벨 분리

---

## 3️⃣ QueryDSL → Native SQL 전환

### 답변: Spring Data JPA의 @Query 사용 권장

**현재 문제:**
- QueryDSL 코드 기반 쿼리 → SQL 가독성 떨어짐
- 복잡한 조회는 SQL이 더 직관적

**해결 방법: @Query + native SQL**

#### Before (QueryDSL)
```kotlin
@Repository
class PostQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PostQueryRepository {

    override fun searchPosts(
        keyword: String?,
        categoryId: Long?,
        status: PostStatus?,
        pageable: Pageable
    ): Page<Post> {
        val builder = BooleanBuilder()

        keyword?.let {
            builder.and(
                post.title.containsIgnoreCase(it)
                    .or(post.content.containsIgnoreCase(it))
            )
        }
        // ... 복잡한 코드
    }
}
```

#### After (@Query + Native SQL)
```kotlin
interface PostRepository : JpaRepository<Post, Long> {

    // 기본 CRUD는 JPA 메서드명 사용
    fun findByStatus(status: PostStatus, pageable: Pageable): Page<Post>
    fun findByCategoryIdAndStatus(categoryId: Long, status: PostStatus, pageable: Pageable): Page<Post>

    // 복잡한 검색은 @Query + Native SQL
    @Query(
        value = """
            SELECT p.*
            FROM posts p
            WHERE (:keyword IS NULL OR
                   LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND p.status = :status
            ORDER BY p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(p.id)
            FROM posts p
            WHERE (:keyword IS NULL OR
                   LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND p.status = :status
        """,
        nativeQuery = true
    )
    fun searchPosts(
        @Param("keyword") keyword: String?,
        @Param("categoryId") categoryId: Long?,
        @Param("status") status: String,
        pageable: Pageable
    ): Page<Post>

    // 인기 게시글
    @Query(
        value = "SELECT * FROM posts WHERE status = 'PUBLISHED' ORDER BY view_count DESC LIMIT :limit",
        nativeQuery = true
    )
    fun findTopByViewCount(@Param("limit") limit: Int): List<Post>
}
```

**장점:**
1. SQL 그대로 작성 → 가독성 최고
2. 성능 최적화 쉬움 (인덱스 활용)
3. DBA와 협업 용이
4. QueryDSL 의존성 제거 가능

**주의사항:**
- nativeQuery = true 사용 시 엔티티 매핑 주의
- Pageable 사용 시 countQuery 필수

**개선 계획:**
1. QueryDSL 의존성 제거
2. 모든 복잡한 조회 쿼리 → @Query + Native SQL 전환
3. 단순 조회는 메서드명 쿼리 유지

---

## 4️⃣ Admin 분리 필요성

### 현재 구조 분석
```
/api/posts          - 공개 게시글 조회
/api/posts/my       - 내 게시글 조회 (인증)
/api/admin/posts    - 관리자 게시글 CRUD
```

### 답변: 회원이 Admin만 있다면 불필요한 분리

**현재 문제:**
- Admin만 회원 가입 가능
- 일반 User는 존재하지 않음
- Admin 분리가 과도한 설계

**개선 방향:**

#### Option 1: Admin API 통합 (권장)
```kotlin
// Before
@RestController
@RequestMapping("/api/admin/posts")
class AdminPostController {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun createPost(...) { }
}

@RestController
@RequestMapping("/api/posts")
class PostController {
    @GetMapping
    fun getAllPosts(...) { }
}

// After (통합)
@RestController
@RequestMapping("/api/posts")
class PostController {

    // 공개 API
    @GetMapping
    fun getAllPosts(...) { }

    @GetMapping("/{postId}")
    fun getPost(...) { }

    @GetMapping("/search")
    fun searchPosts(...) { }

    // 인증 필요 (Admin만 존재하므로 @PreAuthorize 불필요)
    @PostMapping
    fun createPost(@AuthUser userId: Long, @RequestBody request: CreatePostRequest) { }

    @PutMapping("/{postId}")
    fun updatePost(@PathVariable postId: Long, @AuthUser userId: Long, ...) { }

    @DeleteMapping("/{postId}")
    fun deletePost(@PathVariable postId: Long, @AuthUser userId: Long) { }
}
```

#### SecurityConfig 간소화
```kotlin
@Bean
fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
        .authorizeHttpRequests { authorize ->
            authorize
                // 공개 API
                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comments/recent").permitAll()

                // 로그인/토큰 갱신
                .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users/refresh").permitAll()

                // 나머지는 모두 인증 필요 (Admin만 있으므로)
                .anyRequest().authenticated()
        }

    return http.build()
}
```

**장점:**
1. API 구조 단순화
2. /api/admin 제거 → URL 일관성
3. 코드 중복 제거
4. 유지보수 용이

**개선 계획:**
1. AdminPostController → PostController 통합
2. AdminCategoryController → CategoryController 통합
3. @PreAuthorize 제거 (Admin만 존재)
4. SecurityConfig 간소화

---

## 5️⃣ Redis 관리 개선

### 현재 상태
```kotlin
// PostService에 직접 RedisTemplate 사용
@Service
class PostService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    private fun increaseViewCount(postId: Long, clientIp: String) {
        val viewKey = "post:view:$postId:$clientIp"
        val isFirstView = redisTemplate.opsForValue()
            .setIfAbsent(viewKey, "1", 1, TimeUnit.HOURS)
        // ...
    }
}

// RefreshTokenService에도 RedisTemplate 사용
@Service
class RefreshTokenService(
    private val redisTemplate: RedisTemplate<String, String>
) { }
```

### 답변: RedisService 분리 권장

**개선안:**

#### 1. RedisService 생성
```kotlin
@Service
class RedisService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    /**
     * Key가 없을 때만 값 설정 (조회수 중복 방지용)
     * @return true: 새로 설정됨, false: 이미 존재
     */
    fun setIfAbsent(key: String, value: String, timeout: Long, unit: TimeUnit): Boolean {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit) ?: false
    }

    /**
     * 값 저장 (만료 시간 포함)
     */
    fun set(key: String, value: String, timeout: Long, unit: TimeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit)
    }

    /**
     * 값 조회
     */
    fun get(key: String): String? {
        return redisTemplate.opsForValue().get(key)
    }

    /**
     * 값 삭제
     */
    fun delete(key: String): Boolean {
        return redisTemplate.delete(key)
    }

    /**
     * 키 존재 여부 확인
     */
    fun hasKey(key: String): Boolean {
        return redisTemplate.hasKey(key)
    }
}
```

#### 2. 도메인별 Redis 관리 클래스
```kotlin
@Component
class ViewCountRedisManager(
    private val redisService: RedisService
) {
    companion object {
        private const val VIEW_KEY_PREFIX = "post:view:"
        private const val VIEW_EXPIRE_HOURS = 1L
    }

    fun checkAndMarkAsViewed(postId: Long, clientIp: String): Boolean {
        val key = "$VIEW_KEY_PREFIX$postId:$clientIp"
        return redisService.setIfAbsent(key, "1", VIEW_EXPIRE_HOURS, TimeUnit.HOURS)
    }
}

@Component
class RefreshTokenRedisManager(
    private val redisService: RedisService
) {
    companion object {
        private const val REFRESH_TOKEN_PREFIX = "refresh_token:"
        private const val REFRESH_TOKEN_EXPIRE_DAYS = 7L
    }

    fun saveRefreshToken(userId: Long, refreshToken: String) {
        val key = "$REFRESH_TOKEN_PREFIX$userId"
        redisService.set(key, refreshToken, REFRESH_TOKEN_EXPIRE_DAYS, TimeUnit.DAYS)
    }

    fun getRefreshToken(userId: Long): String? {
        val key = "$REFRESH_TOKEN_PREFIX$userId"
        return redisService.get(key)
    }

    fun deleteRefreshToken(userId: Long) {
        val key = "$REFRESH_TOKEN_PREFIX$userId"
        redisService.delete(key)
    }
}
```

#### 3. Service에서 사용
```kotlin
@Service
class PostService(
    private val viewCountRedisManager: ViewCountRedisManager
) {
    private fun increaseViewCount(postId: Long, clientIp: String) {
        try {
            val isFirstView = viewCountRedisManager.checkAndMarkAsViewed(postId, clientIp)
            if (isFirstView) {
                postRepository.incrementViewCount(postId)
            }
        } catch (e: RedisConnectionFailureException) {
            logger.error("Redis connection failed", e)
        }
    }
}
```

**장점:**
1. Redis 로직 중앙 집중 관리
2. 테스트 용이 (Mocking)
3. 키 네이밍 규칙 통일
4. 에러 핸들링 일관성

**개선 계획:**
1. RedisService 생성
2. ViewCountRedisManager, RefreshTokenRedisManager 분리
3. 기존 RedisTemplate 직접 사용 코드 리팩토링

---

## 6️⃣ @AuthUser vs Authentication 통합

### 현재 상태
```kotlin
// PostController
@PostMapping
fun createPost(@AuthUser userId: Long, ...) { }

// UserController
@GetMapping("/me")
fun getMyProfile(authentication: Authentication) {
    val userId = authentication.principal as Long
}
```

### 답변: @AuthUser로 통일 권장

**이유:**

1. **일관성**: 모든 Controller에서 동일한 방식
2. **간결성**: Authentication 캐스팅 불필요
3. **타입 안정성**: Long으로 바로 받음
4. **가독성**: 코드 의도 명확

**현재 @AuthUser 구현**
```kotlin
@Component
class AuthUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthUser::class.java) &&
                parameter.parameterType == Long::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw CustomException(ErrorCode.UNAUTHORIZED)

        return authentication.principal as? Long
            ?: throw CustomException(ErrorCode.UNAUTHORIZED)
    }
}
```

**개선안: 모든 Controller에 @AuthUser 적용**
```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    // Before
    @GetMapping("/me")
    fun getMyProfile(authentication: Authentication) =
        ApiResponse.success(userService.getUserById(authentication.principal as Long))

    // After
    @GetMapping("/me")
    fun getMyProfile(@AuthUser userId: Long) =
        ApiResponse.success(userService.getUserById(userId))

    @PutMapping("/me")
    fun updateProfile(
        @AuthUser userId: Long,
        @RequestBody request: UpdateProfileRequest
    ) = ApiResponse.success(userService.updateProfile(userId, request))

    @PutMapping("/me/password")
    fun changePassword(
        @AuthUser userId: Long,
        @RequestBody request: ChangePasswordRequest
    ): ApiResponse<Unit> {
        userService.changePassword(userId, request)
        return ApiResponse.success(Unit)
    }
}
```

**장점:**
1. 코드 통일성
2. 캐스팅 에러 방지
3. null 체크 한 곳에서 처리
4. Swagger 문서에서 숨김 처리 가능 (@Parameter(hidden = true))

**개선 계획:**
1. UserController의 Authentication → @AuthUser 전환
2. 모든 인증 필요 API에 @AuthUser 적용

---

## 7️⃣ GitHub OAuth 로직 정리

### 현재 문제 분석

#### 1. DTO 중복
```
/domain/auth/dto/
  - GithubUserResponse.kt

/global/web/dto/
  - GitHubUser.kt
```

#### 2. JWT Provider와 GitHub Token 혼용
```kotlin
// JwtAuthenticationFilter - JWT 처리
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider
) {
    // JWT 검증
}

// GitHubAuthArgumentResolver - GitHub Token 처리
class GitHubAuthArgumentResolver(
    private val githubAuthService: GithubAuthService
) {
    // GitHub Token 검증
}
```

### 답변: 명확한 분리 필요

**개선안:**

#### 1. DTO 통합
```kotlin
// domain/auth/dto/GitHubUser.kt (단일화)
data class GitHubUser(
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String?
)

// GitHubUserResponse는 제거하고 위 클래스 재사용
```

#### 2. 인증 방식 명확히 분리

**JWT 인증**: 블로그 관리자 (ADMIN)
- POST /api/users/login → JWT 발급
- 게시글 CRUD, 카테고리 관리 등
- JwtAuthenticationFilter 처리

**GitHub OAuth**: 댓글 작성자 (비회원)
- GET /api/auth/github/callback → GitHub Token 발급
- 댓글 작성/수정/삭제만
- GitHubAuthArgumentResolver 처리

#### 3. 개선된 구조
```kotlin
// 1. JWT 인증 (관리자)
@PostMapping("/api/posts")
fun createPost(@AuthUser adminId: Long, ...) { }

// 2. GitHub OAuth 인증 (댓글 작성자)
@PostMapping("/api/posts/{postId}/comments")
fun createComment(
    @PathVariable postId: Long,
    @GitHubAuth githubUser: GitHubUser,  // GitHub OAuth
    @RequestBody request: CreateCommentRequest
) { }
```

#### 4. 패키지 구조 정리
```
domain/
  auth/
    dto/
      GitHubUser.kt                    (통합)
    service/
      GithubAuthService.kt
    controller/
      AuthController.kt

global/
  security/
    JwtProvider.kt
    JwtAuthenticationFilter.kt
  web/
    annotation/
      AuthUser.kt
      GitHubAuth.kt
    resolver/
      AuthUserArgumentResolver.kt
      GitHubAuthArgumentResolver.kt
```

**개선 계획:**
1. GitHubUserResponse 제거, GitHubUser로 통합
2. global/web/dto 패키지 제거
3. JWT vs GitHub OAuth 역할 명확히 주석 추가
4. 두 인증 방식이 서로 독립적으로 동작하도록 검증

---

## 8️⃣ 에러 메시지 enum 통합 관리

### 현재 상태
```kotlin
// ErrorCode enum에 일부만 관리
enum class ErrorCode(val status: Int, val message: String) {
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다")
}

// 하지만 코드 곳곳에 하드코딩된 메시지들
@field:NotBlank(message = "제목은 필수입니다")
@field:Size(min = 1, max = 200, message = "제목은 1-200자 사이여야 합니다")
```

### 답변: ValidationMessage enum 추가 권장

**개선안:**

#### 1. ValidationMessage enum 생성
```kotlin
enum class ValidationMessage(val message: String) {
    // User
    EMAIL_REQUIRED("이메일은 필수입니다"),
    EMAIL_INVALID("유효하지 않은 이메일 형식입니다"),
    EMAIL_SIZE("이메일은 최대 100자까지 입력 가능합니다"),

    PASSWORD_REQUIRED("비밀번호는 필수입니다"),
    PASSWORD_SIZE("비밀번호는 8-20자 사이여야 합니다"),
    PASSWORD_PATTERN("비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"),

    NICKNAME_REQUIRED("닉네임은 필수입니다"),
    NICKNAME_SIZE("닉네임은 2-50자 사이여야 합니다"),

    // Post
    TITLE_REQUIRED("제목은 필수입니다"),
    TITLE_SIZE("제목은 1-200자 사이여야 합니다"),

    CONTENT_REQUIRED("내용은 필수입니다"),

    CATEGORY_ID_REQUIRED("카테고리는 필수입니다"),

    // Comment
    COMMENT_CONTENT_REQUIRED("댓글 내용은 필수입니다"),
    COMMENT_CONTENT_SIZE("댓글은 1-1000자 사이여야 합니다"),

    // Category
    CATEGORY_NAME_REQUIRED("카테고리 이름은 필수입니다"),
    CATEGORY_NAME_SIZE("카테고리 이름은 1-100자 사이여야 합니다"),

    CATEGORY_SLUG_REQUIRED("카테고리 슬러그는 필수입니다"),
    CATEGORY_SLUG_SIZE("카테고리 슬러그는 1-100자 사이여야 합니다"),
    CATEGORY_SLUG_PATTERN("카테고리 슬러그는 영문 소문자, 숫자, 하이픈만 사용 가능합니다");

    companion object {
        // Validation 어노테이션에서 사용하기 위한 상수
        const val EMAIL_REQUIRED = "이메일은 필수입니다"
        const val PASSWORD_SIZE = "비밀번호는 8-20자 사이여야 합니다"
        // ... 나머지도 동일
    }
}
```

#### 2. DTO에서 사용
```kotlin
// Before
data class SignupRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "유효하지 않은 이메일 형식입니다")
    @field:Size(max = 100, message = "이메일은 최대 100자까지 입력 가능합니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다")
    val password: String
)

// After
data class SignupRequest(
    @field:NotBlank(message = ValidationMessage.EMAIL_REQUIRED)
    @field:Email(message = ValidationMessage.EMAIL_INVALID)
    @field:Size(max = 100, message = ValidationMessage.EMAIL_SIZE)
    val email: String,

    @field:NotBlank(message = ValidationMessage.PASSWORD_REQUIRED)
    @field:Size(min = 8, max = 20, message = ValidationMessage.PASSWORD_SIZE)
    val password: String
)
```

**더 나은 방법: messages.properties 사용**
```properties
# src/main/resources/messages.properties
validation.email.required=이메일은 필수입니다
validation.email.invalid=유효하지 않은 이메일 형식입니다
validation.email.size=이메일은 최대 {max}자까지 입력 가능합니다

validation.password.required=비밀번호는 필수입니다
validation.password.size=비밀번호는 {min}-{max}자 사이여야 합니다
validation.password.pattern=비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다
```

```kotlin
data class SignupRequest(
    @field:NotBlank(message = "{validation.email.required}")
    @field:Email(message = "{validation.email.invalid}")
    @field:Size(max = 100, message = "{validation.email.size}")
    val email: String,

    @field:NotBlank(message = "{validation.password.required}")
    @field:Size(min = 8, max = 20, message = "{validation.password.size}")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]+\$",
        message = "{validation.password.pattern}"
    )
    val password: String
)
```

**장점:**
1. 메시지 중앙 관리
2. 다국어 지원 용이 (messages_en.properties 추가 가능)
3. 메시지 변경 시 재컴파일 불필요
4. 파라미터 바인딩 지원 ({min}, {max})

**개선 계획:**
1. messages.properties 생성
2. 모든 validation 메시지 이동
3. ErrorCode에는 비즈니스 에러만 관리
4. 다국어 준비 (messages_en.properties)

---

## 9️⃣ S3 제거 및 Railway/Vercel 배포 전략

### 현재 상황
- Railway: 백엔드 배포
- Vercel: 프론트엔드 배포
- 이미지 업로드 필요

### 답변: Cloudinary 또는 Railway Volumes 사용

#### Option 1: Cloudinary (권장)
**무료 플랜:**
- 25 Credits/month
- 25GB Storage
- 25GB Bandwidth

**장점:**
1. 무료
2. CDN 제공
3. 이미지 변환 기능 (리사이징, 최적화)
4. Railway/Vercel과 독립적

**구현:**
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.cloudinary:cloudinary-http44:1.36.0")
}

// CloudinaryConfig.kt
@Configuration
class CloudinaryConfig {

    @Bean
    fun cloudinary(
        @Value("\${cloudinary.cloud-name}") cloudName: String,
        @Value("\${cloudinary.api-key}") apiKey: String,
        @Value("\${cloudinary.api-secret}") apiSecret: String
    ): Cloudinary {
        val config = mapOf(
            "cloud_name" to cloudName,
            "api_key" to apiKey,
            "api_secret" to apiSecret
        )
        return Cloudinary(config)
    }
}

// ImageUploadService.kt
@Service
class ImageUploadService(
    private val cloudinary: Cloudinary
) {

    fun uploadImage(file: MultipartFile): String {
        val uploadResult = cloudinary.uploader().upload(
            file.bytes,
            ObjectUtils.asMap(
                "folder", "blog",
                "resource_type", "auto"
            )
        )
        return uploadResult["secure_url"] as String
    }

    fun deleteImage(publicId: String) {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
    }
}

// ImageController.kt
@RestController
@RequestMapping("/api/images")
class ImageController(
    private val imageUploadService: ImageUploadService
) {

    @PostMapping("/upload")
    fun uploadImage(
        @AuthUser userId: Long,
        @RequestParam("file") file: MultipartFile
    ): ApiResponse<String> {
        val imageUrl = imageUploadService.uploadImage(file)
        return ApiResponse.success(imageUrl)
    }
}
```

```yaml
# application.yml
cloudinary:
  cloud-name: ${CLOUDINARY_CLOUD_NAME}
  api-key: ${CLOUDINARY_API_KEY}
  api-secret: ${CLOUDINARY_API_SECRET}
```

#### Option 2: Railway Volumes (파일 저장)
**단점:**
- CDN 없음
- Railway 인스턴스 재시작 시 파일 유지 필요
- 확장성 제한

**추천하지 않음**

#### Option 3: Vercel Blob Storage
**Vercel에서 제공하는 파일 저장소**
- 무료: 1GB
- 프론트엔드에서 직접 업로드 가능

**프론트엔드 구현:**
```typescript
// blog-web/src/utils/uploadImage.ts
import { upload } from '@vercel/blob/client';

export async function uploadImage(file: File): Promise<string> {
  const blob = await upload(file.name, file, {
    access: 'public',
    handleUploadUrl: '/api/upload',
  });

  return blob.url;
}
```

**개선 계획:**
1. Cloudinary 도입 (1순위)
2. S3 관련 코드 제거
3. 이미지 업로드 API 구현
4. 프론트엔드 연동

---

## 🔟 패키지 구조 정리

### 현재 구조 분석
```
src/main/kotlin/com/blog/api/
  domain/
    auth/
    user/
    post/
    comment/
    category/
  global/
    config/
    security/
    web/
    exception/
    response/
    util/
    entity/
```

### 문제점
1. global 패키지가 너무 복잡
2. 역할별 분리 불명확
3. common vs global 혼재

### 답변: 계층별 명확한 분리

**개선안:**

```
src/main/kotlin/com/blog/api/

  domain/                           # 비즈니스 도메인
    auth/
      controller/
        AuthController.kt
      service/
        GithubAuthService.kt
      dto/
        GitHubUser.kt

    user/
      controller/
        UserController.kt
      service/
        UserService.kt
      repository/
        UserRepository.kt
      entity/
        User.kt
      dto/
        SignupRequest.kt
        LoginRequest.kt
        UserResponse.kt

    post/
      controller/
        PostController.kt
      service/
        PostService.kt
      repository/
        PostRepository.kt
      entity/
        Post.kt
        PostStatus.kt
      dto/
        CreatePostRequest.kt
        PostResponse.kt
        PostListResponse.kt

    comment/
      controller/
        CommentController.kt
        CommentPublicController.kt
      service/
        CommentService.kt
      repository/
        CommentRepository.kt
      entity/
        Comment.kt
      dto/
        CreateCommentRequest.kt
        CommentResponse.kt

    category/
      controller/
        CategoryController.kt
      service/
        CategoryService.kt
      repository/
        CategoryRepository.kt
      entity/
        Category.kt
      dto/
        CreateCategoryRequest.kt
        CategoryResponse.kt

  common/                           # 공통 인프라 (global 제거)
    config/
      SecurityConfig.kt
      RedisConfig.kt
      WebMvcConfig.kt
      OpenApiConfig.kt

    security/
      jwt/
        JwtProvider.kt
        JwtAuthenticationFilter.kt
      redis/
        RedisService.kt
        ViewCountRedisManager.kt
        RefreshTokenRedisManager.kt

    web/
      annotation/
        AuthUser.kt
        GitHubAuth.kt
        ClientIp.kt
      resolver/
        AuthUserArgumentResolver.kt
        GitHubAuthArgumentResolver.kt
        ClientIpArgumentResolver.kt
      response/
        ApiResponse.kt
        ErrorResponse.kt

    exception/
      CustomException.kt
      ErrorCode.kt
      GlobalExceptionHandler.kt

    util/
      MarkdownUtil.kt

    entity/
      BaseTimeEntity.kt
```

**핵심 변경사항:**

1. **global → common**
   - 더 명확한 의미
   - Spring Boot 공식 네이밍 컨벤션

2. **security 하위 분류**
   - jwt/: JWT 관련
   - redis/: Redis 관련

3. **web 하위 정리**
   - annotation/
   - resolver/
   - response/

**개선 계획:**
1. global 패키지 → common 리네이밍
2. 하위 패키지 재구조화
3. Import 경로 일괄 수정

---

## 1️⃣1️⃣ DTO 변환 로직 위치

### 현재 상태
```kotlin
// DTO에 from/to 메서드
data class UserResponse(
    val id: Long,
    val email: String,
    val nickname: String
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                email = user.email,
                nickname = user.nickname
            )
        }
    }
}
```

### 답변: 현재 방식이 가장 적합

**이유:**

1. **도메인 독립성**: Entity는 DTO를 몰라야 함
2. **단방향 의존성**: DTO → Entity (OK), Entity → DTO (BAD)
3. **응집도**: Response DTO는 자신의 생성 로직을 가짐

**다른 옵션 분석:**

#### Option 1: Entity에 toDto() (❌ 비권장)
```kotlin
@Entity
class User {
    fun toDto(): UserResponse {  // ❌ Entity가 DTO를 알게 됨
        return UserResponse(id!!, email, nickname)
    }
}
```
**문제:**
- Entity가 Presentation 계층에 의존
- 순환 참조 가능성
- Entity 변경 시 DTO도 영향

#### Option 2: Mapper 클래스 분리 (중립)
```kotlin
@Component
class UserMapper {
    fun toResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id!!,
            email = user.email,
            nickname = user.nickname
        )
    }

    fun toEntity(request: SignupRequest): User {
        return User(
            email = request.email,
            password = request.password,
            nickname = request.nickname
        )
    }
}
```
**장점:**
- 변환 로직 중앙 관리
- 복잡한 변환 로직에 유리

**단점:**
- Mapper 클래스 증가
- Companion object만으로 충분한 경우 과도

#### Option 3: 현재 방식 (✅ 권장)
```kotlin
data class UserResponse(...) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(...)
        }
    }
}

data class SignupRequest(...) {
    fun toEntity(encodedPassword: String): User {
        return User(
            email = this.email,
            password = encodedPassword,
            nickname = this.nickname
        )
    }
}
```

**언제 Mapper를 사용할까?**

**복잡한 변환 로직이 있을 때:**
```kotlin
// 여러 Entity를 조합하는 경우
@Component
class PostMapper(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository
) {
    fun toDetailResponse(post: Post): PostDetailResponse {
        val author = userRepository.findById(post.userId).orElseThrow()
        val category = categoryRepository.findById(post.categoryId).orElseThrow()

        return PostDetailResponse(
            id = post.id!!,
            title = post.title,
            content = post.content,
            author = UserResponse.from(author),
            category = CategoryResponse.from(category),
            createdAt = post.createdAt
        )
    }
}
```

**결론:**
- **단순 변환**: DTO Companion object (현재 방식 유지)
- **복잡한 변환**: Mapper 클래스 분리
- **Entity에는 절대 DTO 로직 넣지 않기**

**개선 계획:**
1. 현재 DTO from/to 방식 유지
2. 복잡한 변환 로직 발생 시 Mapper 도입 고려
3. Entity는 순수하게 유지

---

## 🎯 전체 리팩토링 우선순위

### Phase 1: 즉시 개선 (1-2일)
1. ✅ @AuthUser vs Authentication 통합
2. ✅ Admin API 통합 (불필요한 분리 제거)
3. ✅ global → common 패키지 리네이밍
4. ✅ 에러 메시지 messages.properties 이동

### Phase 2: 핵심 개선 (3-5일)
5. ✅ QueryDSL → @Query Native SQL 전환
6. ✅ RedisService 분리 및 도메인별 Manager 생성
7. ✅ GitHub OAuth DTO 통합
8. ✅ Logging AOP 추가

### Phase 3: 인프라 개선 (1주)
9. ✅ Cloudinary 이미지 업로드 구현
10. ✅ S3 관련 코드 제거
11. ✅ Railway/Vercel 배포 설정

### Phase 4: 문서화 및 테스트 (진행 중)
12. ✅ API 문서 업데이트
13. ⏳ 통합 테스트 추가
14. ⏳ 성능 테스트 (JMeter)

---

## 📝 작업 시작

모든 질문에 대한 답변과 계획을 정리했습니다.
다음 단계로 Phase 1부터 순차적으로 리팩토링을 진행하겠습니다.
