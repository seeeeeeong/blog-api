package com.blog.api.core.domain

import com.blog.api.core.support.connector.RedisOps
import com.blog.api.storage.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
class PostViewService(
    private val redisOps: RedisOps,
    private val postRepository: PostRepository,
    private val postRankingService: PostRankingService,
    @param:Value("\${view-count.ttl-seconds:3600}") private val ttlSeconds: Long,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun incrementIfNeeded(postId: Long, clientIp: String) {
        val key = buildKey(postId, clientIp)
        val isNew = redisOps.setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS)
        if (isNew == null) {
            logger.warn("view-count: Redis unavailable, skipping increment for postId={}", postId)
            return
        }
        if (isNew) {
            postRepository.incrementViewCount(postId)
            postRankingService.incrementScore(postId)
        }
    }

    private fun buildKey(postId: Long, clientIp: String): String {
        val ip = clientIp.trim().ifBlank { "unknown" }
        return "post:view:$postId:$ip"
    }
}
