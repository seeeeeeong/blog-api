package com.blog.api.core.domain

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.connector.RedisOps
import com.blog.api.core.support.properties.PostRankingProperties
import com.blog.api.storage.PostEntity
import com.blog.api.storage.PostRepository
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration

class PostRankingServiceTest {

    /** Redis 장애 시 로컬 캐시가 채워진 이후엔 DB 를 다시 조회하지 않는다 */
    @Test
    fun `Redis 장애 후 로컬 캐시를 우선 사용한다`() {
        val postRepository = mockPostRepository(limit = 3)

        // CB: 최소 2번 호출 후 100% 실패 → OPEN, 테스트 동안 OPEN 유지
        val service = createService(
            redisOps = failingRedisOps(),
            postRepository = postRepository,
            cbWaitMs = 30_000,
        )

        val first = service.getTopPostIds(3)   // redis 실패 → DB 조회 후 로컬 캐시 적재
        val second = service.getTopPostIds(3)  // redis 실패 → 로컬 캐시 hit (DB 재조회 없음)

        assertEquals(listOf(3L, 2L, 1L), first)
        assertEquals(first, second)
        verify(postRepository, times(1)).findByStatus(
            PostStatus.PUBLISHED,
            PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "viewCount"))
        )
    }

    /** CB 가 OPEN 된 후 대기시간이 지나면 HALF_OPEN → 성공 probe → CLOSED 로 복구된다 */
    @Test
    fun `CB 복구 후 Redis 를 다시 사용한다`() {
        var redisAvailable = false
        val redisOps = switchableRedisOps(
            available = { redisAvailable },
            result = { listOf("3", "2", "1") },
        )
        val postRepository = mockPostRepository(limit = 3)

        // CB: 2번 실패 → OPEN, 200ms 후 HALF_OPEN 자동 전환
        val service = createService(
            redisOps = redisOps,
            postRepository = postRepository,
            cbWaitMs = 200,
        )

        // 2번 실패 → CB OPEN
        repeat(2) { service.getTopPostIds(3) }

        // 대기 후 HALF_OPEN 전환
        Thread.sleep(300)

        // Redis 복구 → HALF_OPEN probe 성공 → CB CLOSED
        redisAvailable = true
        val afterRecovery = service.getTopPostIds(3)

        assertEquals(listOf(3L, 2L, 1L), afterRecovery)
        assertEquals(CircuitBreaker.State.CLOSED, extractCbState(service))
        // DB 조회는 장애 첫 번째 호출에서만 발생 (이후 로컬 캐시 hit)
        verify(postRepository, times(1)).findByStatus(
            PostStatus.PUBLISHED,
            PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "viewCount"))
        )
    }

    @Test
    fun `CB OPEN이면 write 연산을 skip한다`() {
        val redisOps = RecordingRedisOps(
            available = { false },
            result = { listOf("3", "2", "1") },
        )
        val service = createService(
            redisOps = redisOps,
            postRepository = mockPostRepository(limit = 3),
            cbWaitMs = 30_000,
        )

        repeat(2) { service.getTopPostIds(3) } // 2회 실패로 OPEN 전환
        assertEquals(CircuitBreaker.State.OPEN, extractCircuitBreaker(service).state)

        service.incrementScore(1L)
        service.remove(2L)
        service.seed(3L, 300)

        assertEquals(0, redisOps.zincrbyCalls)
        assertEquals(0, redisOps.zremCalls)
        assertEquals(0, redisOps.zaddCalls)
    }

    @Test
    fun `CB HALF_OPEN에서는 write 연산을 허용한다`() {
        val redisOps = RecordingRedisOps(
            available = { true },
            result = { listOf("3", "2", "1") },
        )
        val service = createService(
            redisOps = redisOps,
            postRepository = mockPostRepository(limit = 3),
            cbWaitMs = 30_000,
        )

        extractCircuitBreaker(service).apply {
            transitionToOpenState()
            transitionToHalfOpenState()
        }

        service.incrementScore(1L)
        service.remove(2L)
        service.seed(3L, 300)

        assertEquals(1, redisOps.zincrbyCalls)
        assertEquals(1, redisOps.zremCalls)
        assertEquals(1, redisOps.zaddCalls)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun createService(
        redisOps: RedisOps,
        postRepository: PostRepository,
        cbWaitMs: Long,
    ): PostRankingService {
        val cbConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(100f)
            .waitDurationInOpenState(Duration.ofMillis(cbWaitMs))
            .permittedNumberOfCallsInHalfOpenState(1)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build()

        return PostRankingService(
            redisOps = redisOps,
            postRepository = postRepository,
            properties = PostRankingProperties(
                localFallbackTtlSeconds = 60,
                localFallbackMaxSize = 50,
            ),
            circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig),
        )
    }

    private fun mockPostRepository(limit: Int): PostRepository {
        val posts = (limit downTo 1).map { post(id = it.toLong(), viewCount = it * 100) }
        val pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount"))
        return mock(PostRepository::class.java).also { repo ->
            Mockito.`when`(repo.findByStatus(PostStatus.PUBLISHED, pageRequest))
                .thenReturn(PageImpl(posts))
        }
    }

    private fun post(id: Long, viewCount: Int) = PostEntity(
        id = id,
        userId = 1L,
        categoryId = 1L,
        title = "title-$id",
        content = "content-$id",
        viewCount = viewCount,
        status = PostStatus.PUBLISHED,
    )

    @Suppress("UNCHECKED_CAST")
    private fun failingRedisOps(): RedisOps =
        object : RedisOps(mock(RedisTemplate::class.java) as RedisTemplate<String, String>) {
            override fun zrevrange(key: String, start: Long, end: Long): List<String> =
                throw RuntimeException("redis down")
        }

    @Suppress("UNCHECKED_CAST")
    private fun switchableRedisOps(
        available: () -> Boolean,
        result: () -> List<String>,
    ): RedisOps = object : RedisOps(mock(RedisTemplate::class.java) as RedisTemplate<String, String>) {
        override fun zrevrange(key: String, start: Long, end: Long): List<String> =
            if (available()) result() else throw RuntimeException("redis down")
    }

    /** 리플렉션 없이 CB 상태를 확인하기 위해 registry 를 직접 쓰는 대신
     *  CircuitBreaker.decorateSupplier 에서 CallNotPermittedException 이 나오지 않으면 CLOSED 로 간주.
     *  여기서는 명시적 확인을 위해 필드에 노출된 cb 를 통해 검증한다. */
    private fun extractCbState(service: PostRankingService): CircuitBreaker.State {
        return extractCircuitBreaker(service).state
    }

    private fun extractCircuitBreaker(service: PostRankingService): CircuitBreaker {
        val field = PostRankingService::class.java.getDeclaredField("cb")
        field.isAccessible = true
        return field.get(service) as CircuitBreaker
    }

    @Suppress("UNCHECKED_CAST")
    private class RecordingRedisOps(
        private val available: () -> Boolean,
        private val result: () -> List<String>,
    ) : RedisOps(mock(RedisTemplate::class.java) as RedisTemplate<String, String>) {
        var zincrbyCalls: Int = 0
        var zremCalls: Int = 0
        var zaddCalls: Int = 0

        override fun zrevrange(key: String, start: Long, end: Long): List<String> {
            if (!available()) throw RuntimeException("redis down")
            return result()
        }

        override fun zincrby(key: String, delta: Double, member: String) {
            zincrbyCalls++
        }

        override fun zadd(key: String, score: Double, member: String) {
            zaddCalls++
        }

        override fun zrem(key: String, member: String) {
            zremCalls++
        }
    }
}
