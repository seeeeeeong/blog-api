package com.blog.api.storage

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class ImageUploadTokenRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val KEY_PREFIX = "img:upload:token:"
    }

    fun save(uploadToken: String, userId: Long, key: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(
            redisKey(uploadToken),
            buildValue(userId, key),
            ttlSeconds,
            TimeUnit.SECONDS
        )
    }

    fun consume(uploadToken: String, userId: Long, key: String): Boolean {
        val actual = redisTemplate.opsForValue().getAndDelete(redisKey(uploadToken)) ?: return false
        return actual == buildValue(userId, key)
    }

    private fun redisKey(uploadToken: String): String = "$KEY_PREFIX$uploadToken"

    private fun buildValue(userId: Long, key: String): String = "$userId::$key"
}
