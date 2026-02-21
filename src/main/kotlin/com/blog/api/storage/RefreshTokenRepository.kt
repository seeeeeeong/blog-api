package com.blog.api.storage

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class RefreshTokenRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val TOKEN_KEY = "rt:"
    }

    fun save(tokenId: String, userId: Long, ttlSeconds: Long) {
        redisTemplate.opsForValue().set("$TOKEN_KEY$tokenId", userId.toString(), ttlSeconds, TimeUnit.SECONDS)
    }

    fun findUserIdByTokenId(tokenId: String): Long? {
        return redisTemplate.opsForValue().get("$TOKEN_KEY$tokenId")?.toLongOrNull()
    }

    fun delete(tokenId: String) {
        redisTemplate.delete("$TOKEN_KEY$tokenId")
    }
}
