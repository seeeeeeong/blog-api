package com.blog.api.core.domain

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.connector.RedisOps
import com.blog.api.storage.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostRankingService(
    private val redisOps: RedisOps,
    private val postRepository: PostRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val RANKING_KEY = "post:ranking"
    }

    fun getTopPostIds(limit: Int): List<Long> {
        val normalizedLimit = limit.coerceAtLeast(1)
        return try {
            val ids = redisOps.zrevrange(RANKING_KEY, 0, normalizedLimit - 1L)
                .mapNotNull { it.toLongOrNull() }
            if (ids.isEmpty()) getTopIdsFromDb(normalizedLimit) else ids
        } catch (e: Exception) {
            logger.warn("ranking: Redis error, falling back to DB: {}", e.message)
            getTopIdsFromDb(normalizedLimit)
        }
    }

    fun incrementScore(postId: Long) {
        try {
            redisOps.zincrby(RANKING_KEY, 1.0, postId.toString())
        } catch (e: Exception) {
            logger.warn("ranking: failed to increment score for postId={}: {}", postId, e.message)
        }
    }

    fun remove(postId: Long) {
        try {
            redisOps.zrem(RANKING_KEY, postId.toString())
        } catch (e: Exception) {
            logger.warn("ranking: failed to remove postId={}: {}", postId, e.message)
        }
    }

    fun seed(postId: Long, viewCount: Int) {
        try {
            redisOps.zadd(RANKING_KEY, viewCount.toDouble(), postId.toString())
        } catch (e: Exception) {
            logger.warn("ranking: failed to seed postId={}: {}", postId, e.message)
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    @Transactional(readOnly = true)
    fun initialize() {
        try {
            redisOps.delete(RANKING_KEY)
            val posts = postRepository.findByStatus(
                PostStatus.PUBLISHED,
                PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "viewCount")),
            )
            posts.content.forEach { post ->
                redisOps.zadd(RANKING_KEY, post.viewCount.toDouble(), post.id!!.toString())
            }
            logger.info("ranking: initialized {} posts", posts.numberOfElements)
        } catch (e: Exception) {
            logger.warn("ranking: failed to initialize on startup: {}", e.message)
        }
    }

    private fun getTopIdsFromDb(limit: Int): List<Long> {
        val posts = postRepository.findByStatus(
            PostStatus.PUBLISHED,
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount")),
        ).content

        try {
            posts.forEach { post ->
                redisOps.zadd(RANKING_KEY, post.viewCount.toDouble(), post.id!!.toString())
            }
            logger.info("ranking: reseeded {} posts to Redis", posts.size)
        } catch (e: Exception) {
            logger.warn("ranking: reseed to Redis failed: {}", e.message)
        }

        return posts.map { it.id!! }
    }
}
