package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.LoginRateLimitProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class UserLoginRateLimitService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val properties: LoginRateLimitProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val KEY_PREFIX = "login:fail:"
    }

    fun checkOrThrow(clientIp: String, email: String) {
        val key = buildKey(clientIp, email)
        val count = try {
            redisTemplate.opsForValue().get(key)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            logger.warn("login-rate-limit.check failed, allowing request: {}", e.message)
            return
        }
        if (count >= properties.maxFailures) {
            throw CoreException(ErrorType.LOGIN_RATE_LIMITED)
        }
    }

    fun recordFailure(clientIp: String, email: String) {
        val key = buildKey(clientIp, email)
        try {
            val count = redisTemplate.opsForValue().increment(key) ?: return
            if (count == 1L) {
                redisTemplate.expire(key, properties.windowSeconds, TimeUnit.SECONDS)
            }
        } catch (e: Exception) {
            logger.warn("login-rate-limit.recordFailure failed: {}", e.message)
        }
    }

    fun clearFailures(clientIp: String, email: String) {
        val key = buildKey(clientIp, email)
        try {
            redisTemplate.delete(key)
        } catch (e: Exception) {
            logger.warn("login-rate-limit.clearFailures failed: {}", e.message)
        }
    }

    private fun buildKey(clientIp: String, email: String): String {
        val ip = clientIp.trim().ifBlank { "unknown" }
        val normalizedEmail = email.trim().lowercase()
        return "$KEY_PREFIX$ip:$normalizedEmail"
    }
}
