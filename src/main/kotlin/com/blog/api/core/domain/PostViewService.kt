package com.blog.api.core.domain

import com.blog.api.core.support.connector.RedisOps
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class PostViewService(
    private val redisOps: RedisOps,
    private val postRankingService: PostRankingService,
    @param:Value("\${view-count.ttl-seconds:3600}") private val ttlSeconds: Long,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val BATCH_KEY = "post:view:batch"
    }

    fun incrementIfNeeded(postId: Long, clientIp: String) {
        val key = buildKey(postId, clientIp)
        val isNew = redisOps.setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS)
        if (isNew == null) {
            logger.warn("view-count: Redis unavailable, skipping increment for postId={}", postId)
            return
        }
        if (isNew) {
            redisOps.zincrby(BATCH_KEY, 1.0, postId.toString())
            postRankingService.incrementScore(postId)
        }
    }

    private fun buildKey(postId: Long, clientIp: String): String {
        val ip = clientIp.trim().ifBlank { "unknown" }
        return "post:view:$postId:$ip"
    }
}
