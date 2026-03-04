package com.blog.api.core.domain

import com.blog.api.core.support.connector.RedisOps
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.TimeUnit

class PostViewServiceTest {

    @Test
    fun `새로운 방문자 - 조회수 배치 키 증가 및 랭킹 업데이트`() {
        val zincrbyKeys = mutableListOf<String>()
        val (service, postRankingService) = fixture(
            setIfAbsentResult = true,
            zincrbyCapture = { key, _, _ -> zincrbyKeys.add(key) },
        )

        service.incrementIfNeeded(1L, "127.0.0.1")

        assertEquals(listOf(PostViewService.BATCH_KEY), zincrbyKeys)
        verify(postRankingService).incrementScore(1L)
    }

    @Test
    fun `중복 방문자 - 아무것도 증가 안 함`() {
        val zincrbyKeys = mutableListOf<String>()
        val (service, postRankingService) = fixture(
            setIfAbsentResult = false,
            zincrbyCapture = { key, _, _ -> zincrbyKeys.add(key) },
        )

        service.incrementIfNeeded(1L, "127.0.0.1")

        assertEquals(emptyList<String>(), zincrbyKeys)
        verify(postRankingService, never()).incrementScore(1L)
    }

    @Test
    fun `Redis 장애 - 조회수 skip (fail-open)`() {
        val zincrbyKeys = mutableListOf<String>()
        val (service, postRankingService) = fixture(
            setIfAbsentResult = null,
            zincrbyCapture = { key, _, _ -> zincrbyKeys.add(key) },
        )

        service.incrementIfNeeded(1L, "127.0.0.1")

        assertEquals(emptyList<String>(), zincrbyKeys)
        verify(postRankingService, never()).incrementScore(1L)
    }

    @Suppress("UNCHECKED_CAST")
    private fun fixture(
        setIfAbsentResult: Boolean?,
        zincrbyCapture: (String, Double, String) -> Unit = { _, _, _ -> },
    ): Pair<PostViewService, PostRankingService> {
        val redisOps = object : RedisOps(mock(RedisTemplate::class.java) as RedisTemplate<String, String>) {
            override fun setIfAbsent(key: String, value: String, timeout: Long, unit: TimeUnit) = setIfAbsentResult
            override fun zincrby(key: String, delta: Double, member: String) = zincrbyCapture(key, delta, member)
        }
        val postRankingService = mock(PostRankingService::class.java)
        val service = PostViewService(redisOps, postRankingService, ttlSeconds = 3600)
        return service to postRankingService
    }
}
