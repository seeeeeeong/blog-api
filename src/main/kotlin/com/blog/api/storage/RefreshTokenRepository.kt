package com.blog.api.storage

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class RefreshTokenRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val TOKEN_KEY = "auth:refresh-token:"
    }

    fun save(tokenId: String, userId: Long, ttlSeconds: Long) {
        try {
            redisTemplate.opsForValue().set("$TOKEN_KEY$tokenId", userId.toString(), ttlSeconds, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw redisUnavailable("refresh-token.save", e)
        }
    }

    fun findUserIdByTokenId(tokenId: String): Long? {
        return try {
            redisTemplate.opsForValue().get("$TOKEN_KEY$tokenId")?.toLongOrNull()
        } catch (e: Exception) {
            throw redisUnavailable("refresh-token.find", e)
        }
    }

    fun delete(tokenId: String) {
        try {
            redisTemplate.delete("$TOKEN_KEY$tokenId")
        } catch (e: Exception) {
            throw redisUnavailable("refresh-token.delete", e)
        }
    }

    private fun redisUnavailable(action: String, cause: Exception): CoreException {
        return CoreException(
            errorType = ErrorType.REDIS_UNAVAILABLE,
            message = "Redis unavailable during $action",
            cause = cause,
        )
    }
}
