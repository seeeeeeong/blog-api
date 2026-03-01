package com.blog.api.core.domain

import com.blog.api.core.support.connector.RedisOps
import com.blog.api.storage.PostRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.TimeUnit

class PostViewServiceTest {

    @Test
    fun `새로운 방문자 - 조회수 증가`() {
        val (service, postRepository, postRankingService) = fixture(setIfAbsentResult = true)

        service.incrementIfNeeded(1L, "127.0.0.1")

        verify(postRepository).incrementViewCount(1L)
        verify(postRankingService).incrementScore(1L)
    }

    @Test
    fun `중복 방문자 - 조회수 증가 안 함`() {
        val (service, postRepository, postRankingService) = fixture(setIfAbsentResult = false)

        service.incrementIfNeeded(1L, "127.0.0.1")

        verify(postRepository, never()).incrementViewCount(anyLong())
        verify(postRankingService, never()).incrementScore(anyLong())
    }

    @Test
    fun `Redis 장애 - 조회수 skip (fail-open)`() {
        val (service, postRepository, postRankingService) = fixture(setIfAbsentResult = null)

        service.incrementIfNeeded(1L, "127.0.0.1")

        verify(postRepository, never()).incrementViewCount(anyLong())
        verify(postRankingService, never()).incrementScore(anyLong())
    }

    @Suppress("UNCHECKED_CAST")
    private fun fixture(setIfAbsentResult: Boolean?): Triple<PostViewService, PostRepository, PostRankingService> {
        val redisOps = object : RedisOps(mock(RedisTemplate::class.java) as RedisTemplate<String, String>) {
            override fun setIfAbsent(key: String, value: String, timeout: Long, unit: TimeUnit) = setIfAbsentResult
        }
        val postRepository = mock(PostRepository::class.java)
        val postRankingService = mock(PostRankingService::class.java)
        val service = PostViewService(redisOps, postRepository, postRankingService, ttlSeconds = 3600)
        return Triple(service, postRepository, postRankingService)
    }
}
