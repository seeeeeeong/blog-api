package com.blog.api.core.domain.post

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PostCacheEvictListener(
    private val cacheManager: CacheManager,
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: PostCacheEvictEvent) {
        evictHtmlCache(event.postId)
        cacheManager.getCache("popular-posts")?.clear()
    }

    private fun evictHtmlCache(postId: Long) {
        try {
            cacheManager.getCache("post-html")?.evict(postId)
        } catch (e: Exception) {
            log.warn(e) { "Cache evict failed: postId=$postId" }
        }
    }
}
