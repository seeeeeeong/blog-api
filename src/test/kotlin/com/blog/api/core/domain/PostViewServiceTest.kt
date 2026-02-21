package com.blog.api.core.domain

import com.blog.api.core.support.connector.RedisOps
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
import java.util.concurrent.TimeUnit

class PostViewServiceTest {

    @Test
    fun `redis success should deduplicate by redis key`() {
        val fixture = fixture(
            ttlSeconds = 60,
            failureThreshold = 3,
            openDurationMs = 30_000,
        ) { valueOps ->
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
        val fixture = fixture(
            ttlSeconds = 1,
            failureThreshold = 1,
            openDurationMs = 5_000,
        ) { valueOps ->
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
        assertThat(third).isTrue()
        assertThat(fourth).isFalse()
        verify(fixture.valueOps, times(1))
            .setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS))
    }

    @Test
    fun `circuit should retry redis after open duration`() {
        val fixture = fixture(
            ttlSeconds = 60,
            failureThreshold = 1,
            openDurationMs = 150,
        ) { valueOps ->
            `when`(valueOps.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(RuntimeException("redis timeout"))
                .thenReturn(true)
        }

        val first = fixture.service.shouldIncrement(10L, "192.168.0.1")
        val second = fixture.service.shouldIncrement(10L, "192.168.0.1")
        Thread.sleep(200)
        val third = fixture.service.shouldIncrement(10L, "192.168.0.2")

        assertThat(first).isTrue()
        assertThat(second).isFalse()
        assertThat(third).isTrue()
        verify(fixture.valueOps, times(2))
            .setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS))
    }

    private fun fixture(
        ttlSeconds: Long,
        failureThreshold: Int,
        openDurationMs: Long,
        maxEntries: Int = 10_000,
        stub: (ValueOperations<String, String>) -> Unit,
    ): Fixture {
        @Suppress("UNCHECKED_CAST")
        val redisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, String>
        @Suppress("UNCHECKED_CAST")
        val valueOps = mock(ValueOperations::class.java) as ValueOperations<String, String>
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        stub(valueOps)

        val service = PostViewService(
            redisOps = RedisOps(redisTemplate),
            ttlSeconds = ttlSeconds,
            failureThreshold = failureThreshold,
            openDurationMs = openDurationMs,
            localCacheMaxEntries = maxEntries,
        )

        return Fixture(service, valueOps)
    }

    private data class Fixture(
        val service: PostViewService,
        val valueOps: ValueOperations<String, String>,
    )
}
