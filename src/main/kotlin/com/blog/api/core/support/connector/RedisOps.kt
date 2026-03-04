package com.blog.api.core.support.connector

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedisOps(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val logger = LoggerFactory.getLogger(RedisOps::class.java)

    /** null = Redis 오류 (fail-open 여부는 호출부 판단) */
    fun setIfAbsent(key: String, value: String, timeout: Long, unit: TimeUnit): Boolean? {
        return try {
            redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit)
        } catch (e: Exception) {
            logger.warn("Redis setIfAbsent failed: key=$key, error=${e.message}")
            null
        }
    }

    fun set(key: String, value: String, timeout: Long, unit: TimeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit)
        } catch (e: Exception) {
            logger.warn("Redis set failed: key=$key, error=${e.message}")
        }
    }

    fun get(key: String): String? {
        return try {
            redisTemplate.opsForValue().get(key)
        } catch (e: Exception) {
            logger.warn("Redis get failed: key=$key, error=${e.message}")
            null
        }
    }

    fun delete(key: String) {
        try {
            redisTemplate.delete(key)
        } catch (e: Exception) {
            logger.warn("Redis delete failed: key=$key, error=${e.message}")
        }
    }

    /** 실패 시 throw — 호출부에서 catch 후 fallback 처리 */
    fun zrevrange(key: String, start: Long, end: Long): List<String> {
        return redisTemplate.opsForZSet().reverseRange(key, start, end)?.toList() ?: emptyList()
    }

    fun zincrby(key: String, delta: Double, member: String) {
        try {
            redisTemplate.opsForZSet().incrementScore(key, member, delta)
        } catch (e: Exception) {
            logger.warn("Redis ZINCRBY failed: key=$key, error=${e.message}")
        }
    }

    fun zadd(key: String, score: Double, member: String) {
        try {
            redisTemplate.opsForZSet().add(key, member, score)
        } catch (e: Exception) {
            logger.warn("Redis ZADD failed: key=$key, error=${e.message}")
        }
    }

    fun zrem(key: String, member: String) {
        try {
            redisTemplate.opsForZSet().remove(key, member)
        } catch (e: Exception) {
            logger.warn("Redis ZREM failed: key=$key, error=${e.message}")
        }
    }

    /** null = Redis 오류 */
    fun zrangeWithScores(key: String, start: Long, end: Long): Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>>? {
        return try {
            redisTemplate.opsForZSet().rangeWithScores(key, start, end)
        } catch (e: Exception) {
            logger.warn("Redis ZRANGEWITHSCORES failed: key=$key, error=${e.message}")
            null
        }
    }
}
