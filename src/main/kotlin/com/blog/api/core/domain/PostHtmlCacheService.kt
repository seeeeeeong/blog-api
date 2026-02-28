package com.blog.api.core.domain

import com.blog.api.core.support.converter.PostMarkdownConverter
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class PostHtmlCacheService(
    private val postMarkdownConverter: PostMarkdownConverter,
    private val cacheManager: CacheManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val localCache: Cache<Long, String> = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build()

    fun getHtml(postId: Long, markdown: String): String {
        // L1: Caffeine 로컬 캐시
        localCache.getIfPresent(postId)?.let { return it }

        // L2: Redis 캐시
        val redisCache = cacheManager.getCache("post-html")
        if (redisCache != null) {
            try {
                val cached = redisCache.get(postId, String::class.java)
                if (cached != null) {
                    localCache.put(postId, cached)
                    return cached
                }
            } catch (e: Exception) {
                logger.warn("[Cache] Redis GET 실패 - postId={}: {}", postId, e.message)
            }
        }

        // L3: 직접 파싱
        val html = postMarkdownConverter.convertToHtml(markdown)
        localCache.put(postId, html)
        if (redisCache != null) {
            try {
                redisCache.put(postId, html)
            } catch (e: Exception) {
                logger.warn("[Cache] Redis PUT 실패 - postId={}: {}", postId, e.message)
            }
        }
        return html
    }

    fun evict(postId: Long) {
        localCache.invalidate(postId)
        val redisCache = cacheManager.getCache("post-html")
        if (redisCache != null) {
            try {
                redisCache.evict(postId)
            } catch (e: Exception) {
                logger.warn("[Cache] Redis EVICT 실패 - postId={}: {}", postId, e.message)
            }
        }
    }
}
