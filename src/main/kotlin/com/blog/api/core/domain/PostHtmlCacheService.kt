package com.blog.api.core.domain

import com.blog.api.core.support.converter.PostMarkdownConverter
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class PostHtmlCacheService(
    private val postMarkdownConverter: PostMarkdownConverter,
    private val cacheManager: CacheManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getHtml(postId: Long, markdown: String): String {
        val cache = cacheManager.getCache("post-html")
            ?: return postMarkdownConverter.convertToHtml(markdown)

        return try {
            cache.get(postId, String::class.java)
                ?: postMarkdownConverter.convertToHtml(markdown).also {
                    cache.put(postId, it)
                }
        } catch (e: Exception) {
            logger.warn("[Cache] local(caffeine) cache failure - postId={}: {}", postId, e.message)
            postMarkdownConverter.convertToHtml(markdown)
        }
    }

    fun evict(postId: Long) {
        try {
            cacheManager.getCache("post-html")?.evict(postId)
        } catch (e: Exception) {
            logger.warn("[Cache] local(caffeine) cache evict failure - postId={}: {}", postId, e.message)
        }
    }
}
