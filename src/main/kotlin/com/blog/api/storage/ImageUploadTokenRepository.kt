package com.blog.api.storage

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
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
        try {
            redisTemplate.opsForValue().set(
                redisKey(uploadToken),
                buildValue(userId, key),
                ttlSeconds,
                TimeUnit.SECONDS
            )
        } catch (e: Exception) {
            throw redisUnavailable("image-upload-token.save", e)
        }
    }

    fun consume(uploadToken: String, userId: Long, key: String): Boolean {
        val actual = try {
            redisTemplate.opsForValue().getAndDelete(redisKey(uploadToken))
        } catch (e: Exception) {
            throw redisUnavailable("image-upload-token.consume", e)
        } ?: return false
        return actual == buildValue(userId, key)
    }

    private fun redisKey(uploadToken: String): String = "$KEY_PREFIX$uploadToken"

    private fun buildValue(userId: Long, key: String): String = "$userId::$key"

    private fun redisUnavailable(action: String, cause: Exception): CoreException {
        return CoreException(
            errorType = ErrorType.REDIS_UNAVAILABLE,
            message = "Redis unavailable during $action",
            cause = cause,
        )
    }
}
