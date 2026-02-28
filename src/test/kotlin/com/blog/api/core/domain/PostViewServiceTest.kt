package com.blog.api.core.domain

import com.blog.api.core.support.connector.RedisOps
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.concurrent.TimeUnit

class PostViewServiceTest {

    @Test
    fun `redis success should deduplicate by redis key`() {
        val fixture = fixture { valueOps ->
            `when`(valueOps.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true)
                .thenReturn(false)
        }

        val first = fixture.service.shouldIncrement(1L, "127.0.0.1")
        val second = fixture.service.shouldIncrement(1L, "127.0.0.1")

        assertThat(first).isTrue()
        assertThat(second).isFalse()
    }

    @Test
    fun `after redis failures local cache fallback should still deduplicate`() {
        // cbWaitMs=30_000: CB stays OPEN well past the 1100ms sleep — no HALF_OPEN probe during test
        val fixture = fixture(ttlSeconds = 1, cbWaitMs = 30_000) { valueOps ->
            `when`(valueOps.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(RuntimeException("redis timeout"))
        }

        val first = fixture.service.shouldIncrement(7L, "10.0.0.1")
        val second = fixture.service.shouldIncrement(7L, "10.0.0.1")
        Thread.sleep(1_100)
        val third = fixture.service.shouldIncrement(7L, "10.0.0.1")
        val fourth = fixture.service.shouldIncrement(7L, "10.0.0.1")

        assertThat(first).isTrue()
        assertThat(second).isFalse()
        assertThat(third).isTrue()   // local cache TTL expired → miss → true
        assertThat(fourth).isFalse() // local cache hit → false
        // CB opens after 2 Redis failures (minimumNumberOfCalls=2); all subsequent calls use local cache
        verify(fixture.valueOps, times(2))
            .setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS))
    }

    @Test
    fun `circuit should retry redis after open duration`() {
        // cbWaitMs=200: CB transitions to HALF_OPEN after 200ms, allowing a retry probe
        val fixture = fixture(cbWaitMs = 200) { valueOps ->
            `when`(valueOps.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(RuntimeException("redis timeout"))  // call 1: failure #1
                .thenThrow(RuntimeException("redis timeout"))  // call 2: failure #2 → CB opens
                .thenReturn(true)                              // call 3: HALF_OPEN probe → CB closes
        }

        val first = fixture.service.shouldIncrement(10L, "192.168.0.1")
        val second = fixture.service.shouldIncrement(10L, "192.168.0.1") // local cache hit → false
        Thread.sleep(300)                                                  // wait past 200ms open window
        val third = fixture.service.shouldIncrement(10L, "192.168.0.2")  // different IP → HALF_OPEN probe

        assertThat(first).isTrue()
        assertThat(second).isFalse()
        assertThat(third).isTrue()
        // 2 failures before CB opens + 1 HALF_OPEN probe = 3 total Redis calls
        verify(fixture.valueOps, times(3))
            .setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS))
    }

    private fun fixture(
        ttlSeconds: Long = 60,
        maxEntries: Long = 10_000,
        cbWaitMs: Long = 30_000,
        stub: (ValueOperations<String, String>) -> Unit,
    ): Fixture {
        @Suppress("UNCHECKED_CAST")
        val redisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, String>
        @Suppress("UNCHECKED_CAST")
        val valueOps = mock(ValueOperations::class.java) as ValueOperations<String, String>
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        stub(valueOps)

        val cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50f)
            .slidingWindowSize(4)
            .minimumNumberOfCalls(2)
            .waitDurationInOpenState(Duration.ofMillis(cbWaitMs))
            .permittedNumberOfCallsInHalfOpenState(1)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build()

        val service = PostViewService(
            redisOps = RedisOps(redisTemplate),
            ttlSeconds = ttlSeconds,
            localCacheMaxEntries = maxEntries,
            circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig),
        )

        return Fixture(service, valueOps)
    }

    private data class Fixture(
        val service: PostViewService,
        val valueOps: ValueOperations<String, String>,
    )
}
