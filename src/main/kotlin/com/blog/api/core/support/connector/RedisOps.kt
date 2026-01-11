package com.blog.api.core.support.connector

import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedisOps(
    private val redisTemplate: RedisTemplate<String, String>,
) {

    private val logger = LoggerFactory.getLogger(RedisOps::class.java)

    fun setIfAbsent(key: String, value: String, timeout: Long, unit: TimeUnit): SetIfAbsentResult {
        return try {
            val isSet = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit) == true
            SetIfAbsentResult(isSet = isSet, isError = false)
        } catch (e: Exception) {
            when (e) {
                is RedisConnectionFailureException -> logger.error("Redis connection failed: key=$key", e)
                else -> logger.warn("Unexpected Redis error: key=$key, error=${e.message}", e)
            }
            SetIfAbsentResult(isSet = false, isError = true)
        }
    }

    fun set(key: String, value: String, timeout: Long, unit: TimeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit)
        } catch (e: Exception) {
            logger.warn("Redis set failed: key=$key, error=${e.message}", e)
        }
    }

    fun get(key: String): String? {
        return try {
            redisTemplate.opsForValue().get(key)
        } catch (e: Exception) {
            logger.warn("Redis get failed: key=$key, error=${e.message}", e)
            null
        }
    }

    fun delete(key: String) {
        try {
            redisTemplate.delete(key)
        } catch (e: Exception) {
            logger.warn("Redis delete failed: key=$key, error=${e.message}", e)
        }
    }

    data class SetIfAbsentResult(
        val isSet: Boolean,
        val isError: Boolean
    )
}
