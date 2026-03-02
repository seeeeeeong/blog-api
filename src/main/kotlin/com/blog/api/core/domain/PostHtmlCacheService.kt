package com.blog.api.core.domain

import com.blog.api.core.support.converter.PostMarkdownConverter
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class PostHtmlCacheService(
    private val postMarkdownConverter: PostMarkdownConverter,
    private val cacheManager: CacheManager,
    meterRegistry: MeterRegistry,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val cacheHit = Counter.builder("blog.cache.html")
        .tag("result", "hit")
        .description("Redis HTML cache hit")
        .register(meterRegistry)

    private val cacheMiss = Counter.builder("blog.cache.html")
        .tag("result", "miss")
        .description("Redis HTML cache miss — converted and stored")
        .register(meterRegistry)

    private val cacheError = Counter.builder("blog.cache.html")
        .tag("result", "error")
        .description("Redis HTML cache error — fallback to convert")
        .register(meterRegistry)

    fun getHtml(postId: Long, markdown: String): String {
        val cache = cacheManager.getCache("post-html")
            ?: return postMarkdownConverter.convertToHtml(markdown).also { cacheMiss.increment() }

        return try {
            cache.get(postId, String::class.java)
                ?.also { cacheHit.increment() }
                ?: postMarkdownConverter.convertToHtml(markdown).also {
                    cacheMiss.increment()
                    cache.put(postId, it)
                }
        } catch (e: Exception) {
            cacheError.increment()
            logger.warn("[Cache] Redis 실패 - postId={}: {}", postId, e.message)
            postMarkdownConverter.convertToHtml(markdown)
        }
    }

    fun evict(postId: Long) {
        try {
            cacheManager.getCache("post-html")?.evict(postId)
        } catch (e: Exception) {
            logger.warn("[Cache] Redis EVICT 실패 - postId={}: {}", postId, e.message)
        }
    }
}
