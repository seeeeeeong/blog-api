package com.blog.api.core.domain

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.connector.RedisOps
import com.blog.api.storage.PostEntity
import com.blog.api.storage.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate

class PostRankingServiceTest {

    @Test
    fun `Redis 정상 시 sorted set 에서 인기글을 조회한다`() {
        val redisOps = fakeRedisOps(result = listOf("3", "2", "1"))
        val service = createService(redisOps = redisOps)

        val ids = service.getTopPostIds(3)

        assertEquals(listOf(3L, 2L, 1L), ids)
    }

    @Test
    fun `Redis 장애 시 DB fallback 으로 인기글을 조회한다`() {
        val redisOps = fakeRedisOps(fail = true)
        val postRepository = mockPostRepository(limit = 3)
        val service = createService(redisOps = redisOps, postRepository = postRepository)

        val ids = service.getTopPostIds(3)

        assertEquals(listOf(3L, 2L, 1L), ids)
        verify(postRepository, times(1)).findByStatus(
            PostStatus.PUBLISHED,
            PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "viewCount")),
        )
    }

    @Test
    fun `Redis 키가 비어있으면 DB fallback 후 Redis 를 reseed 한다`() {
        val redisOps = RecordingRedisOps(result = emptyList())
        val postRepository = mockPostRepository(limit = 3)
        val service = createService(redisOps = redisOps, postRepository = postRepository)

        val ids = service.getTopPostIds(3)

        assertEquals(listOf(3L, 2L, 1L), ids)
        assertEquals(3, redisOps.zaddCalls, "DB fallback 후 Redis 에 reseed 해야 한다")
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun createService(
        redisOps: RedisOps = fakeRedisOps(),
        postRepository: PostRepository = mock(PostRepository::class.java),
    ): PostRankingService {
        return PostRankingService(
            redisOps = redisOps,
            postRepository = postRepository,
        )
    }

    private fun mockPostRepository(limit: Int): PostRepository {
        val posts = (limit downTo 1).map { post(id = it.toLong(), viewCount = it * 100) }
        val pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount"))
        return mock(PostRepository::class.java).also { repo ->
            org.mockito.Mockito.`when`(repo.findByStatus(PostStatus.PUBLISHED, pageRequest))
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
    private fun fakeRedisOps(
        result: List<String> = emptyList(),
        fail: Boolean = false,
    ): RedisOps = object : RedisOps(mock(RedisTemplate::class.java) as RedisTemplate<String, String>) {
        override fun zrevrange(key: String, start: Long, end: Long): List<String> {
            if (fail) throw RuntimeException("redis down")
            return result
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class RecordingRedisOps(
        private val result: List<String>,
    ) : RedisOps(mock(RedisTemplate::class.java) as RedisTemplate<String, String>) {
        var zaddCalls: Int = 0

        override fun zrevrange(key: String, start: Long, end: Long): List<String> = result

        override fun zadd(key: String, score: Double, member: String) {
            zaddCalls++
        }
    }
}
