package com.blog.api.common.redis.service

import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedisBaseService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    private val logger = LoggerFactory.getLogger(RedisBaseService::class.java)

    fun setIfAbsent(key: String, value: String, timeout: Long, unit: TimeUnit): Boolean =
        runCatching {
            redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit) == true
        }.onFailure { e ->
            when (e) {
                is RedisConnectionFailureException -> logger.error("Redis connection failed: key=$key", e)
                else -> logger.warn("Unexpected Redis error: key=$key, error=${e.message}", e)
            }
        }.getOrDefault(false)

    fun set(key: String, value: String, timeout: Long, unit: TimeUnit) =
        runCatching { redisTemplate.opsForValue().set(key, value, timeout, unit) }
            .onFailure { e -> logger.warn("Redis set failed: key=$key, error=${e.message}", e) }

    fun get(key: String): String? =
        runCatching { redisTemplate.opsForValue().get(key) }
            .onFailure { e -> logger.warn("Redis get failed: key=$key, error=${e.message}", e) }
            .getOrNull()

    fun delete(key: String) =
        runCatching { redisTemplate.delete(key) }
            .onFailure { e -> logger.warn("Redis delete failed: key=$key, error=${e.message}", e) }
}