package com.blog.api.core.domain

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.connector.RedisOps
import com.blog.api.core.support.properties.PostRankingProperties
import com.blog.api.storage.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.atomic.AtomicLong

@Service
class PostRankingService(
    private val redisOps: RedisOps,
    private val postRepository: PostRepository,
    private val properties: PostRankingProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val redisDownUntil = AtomicLong(0L)

    companion object {
        private const val RANKING_KEY = "post:ranking"
    }

    fun getTopPostIds(limit: Int): List<Long> {
        if (System.currentTimeMillis() < redisDownUntil.get()) {
            return getTopIdsFromDb(limit, reseed = false)
        }
        return try {
            val ids = redisOps.zrevrange(RANKING_KEY, 0, limit - 1L).map { it.toLong() }
            if (ids.isEmpty()) getTopIdsFromDb(limit, reseed = true) else ids
        } catch (e: Exception) {
            logger.warn("ranking: Redis unavailable, cooling down {}ms, falling back to DB: {}", properties.redisCooldownMs, e.message)
            redisDownUntil.set(System.currentTimeMillis() + properties.redisCooldownMs)
            getTopIdsFromDb(limit, reseed = false)
        }
    }

    fun incrementScore(postId: Long) {
        redisOps.zincrby(RANKING_KEY, 1.0, postId.toString())
    }

    fun remove(postId: Long) {
        redisOps.zrem(RANKING_KEY, postId.toString())
    }

    fun seed(postId: Long, viewCount: Int) {
        redisOps.zadd(RANKING_KEY, viewCount.toDouble(), postId.toString())
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

    private fun getTopIdsFromDb(limit: Int, reseed: Boolean): List<Long> {
        val posts = postRepository.findByStatus(
            PostStatus.PUBLISHED,
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount")),
        ).content
        if (reseed) {
            try {
                posts.forEach { post -> redisOps.zadd(RANKING_KEY, post.viewCount.toDouble(), post.id!!.toString()) }
                logger.info("ranking: reseeded {} posts to Redis after key miss", posts.size)
            } catch (e: Exception) {
                logger.warn("ranking: reseed to Redis failed: {}", e.message)
            }
        }
        return posts.map { it.id!! }
    }
}
