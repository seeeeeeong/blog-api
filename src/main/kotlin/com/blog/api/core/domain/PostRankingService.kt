package com.blog.api.core.domain

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.connector.RedisOps
import com.blog.api.core.support.properties.PostRankingProperties
import com.blog.api.storage.PostRepository
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class PostRankingService(
    private val redisOps: RedisOps,
    private val postRepository: PostRepository,
    private val properties: PostRankingProperties,
    circuitBreakerRegistry: CircuitBreakerRegistry,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val cb = circuitBreakerRegistry.circuitBreaker("post-ranking-redis")
    private val localFallbackCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(properties.localFallbackTtlSeconds.coerceAtLeast(1)))
        .maximumSize(properties.localFallbackMaxSize.coerceAtLeast(1))
        .build<Int, List<Long>>()

    companion object {
        private const val RANKING_KEY = "post:ranking"
    }

    fun getTopPostIds(limit: Int): List<Long> {
        val normalizedLimit = limit.coerceAtLeast(1)
        return try {
            val ids = CircuitBreaker.decorateSupplier(cb) {
                redisOps.zrevrange(RANKING_KEY, 0, normalizedLimit - 1L).mapNotNull { it.toLongOrNull() }
            }.get()

            if (ids.isEmpty()) {
                getTopIdsFromLocalOrDb(normalizedLimit, reseed = true, reason = "redis-key-miss")
            } else {
                localFallbackCache.put(normalizedLimit, ids)
                ids
            }
        } catch (e: CallNotPermittedException) {
            logger.warn("ranking: CB OPEN, using local cache/DB fallback")
            getTopIdsFromLocalOrDb(normalizedLimit, reseed = false, reason = "cb-open")
        } catch (e: Exception) {
            logger.warn("ranking: Redis error [CB state={}], using local cache/DB fallback: {}", cb.state, e.message)
            getTopIdsFromLocalOrDb(normalizedLimit, reseed = false, reason = "redis-error")
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

    private fun getTopIdsFromLocalOrDb(limit: Int, reseed: Boolean, reason: String): List<Long> {
        localFallbackCache.getIfPresent(limit)?.takeIf { it.isNotEmpty() }?.let { cached ->
            logger.info("ranking: served {} posts from local fallback cache ({})", cached.size, reason)
            return cached
        }

        val dbIds = getTopIdsFromDb(limit, reseed)
        if (dbIds.isNotEmpty()) {
            localFallbackCache.put(limit, dbIds)
        }
        return dbIds
    }
}
