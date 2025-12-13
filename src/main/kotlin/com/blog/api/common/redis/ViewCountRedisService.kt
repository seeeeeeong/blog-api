package com.blog.api.common.redis

import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class ViewCountRedisService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    private val logger = LoggerFactory.getLogger(ViewCountRedisService::class.java)

    companion object {
        private const val VIEW_KEY_PREFIX = "post:view:"
        private const val VIEW_EXPIRATION_HOURS = 1L
    }

    fun isFirstView(postId: Long, clientIp: String): Boolean {
        return runCatching {
            val viewKey = getKey(postId, clientIp)
            val isFirstView = redisTemplate.opsForValue()
                .setIfAbsent(viewKey, "1", VIEW_EXPIRATION_HOURS, TimeUnit.HOURS) == true

            if (isFirstView) {
                logger.debug("First view recorded: postId={}, clientIp={}", postId, clientIp)
            }

            isFirstView
        }.onFailure { e ->
            when (e) {
                is RedisConnectionFailureException -> {
                    logger.error("Redis connection failed during view count check: postId={}, clientIp={}",
                        postId, clientIp, e)
                }
                else -> {
                    logger.warn("Unexpected error during view count check: postId={}, clientIp={}, error={}",
                        postId, clientIp, e.message, e)
                }
            }
        }.getOrDefault(false)
    }

    private fun getKey(postId: Long, clientIp: String): String {
        return "$VIEW_KEY_PREFIX$postId:$clientIp"
    }
}
