package com.blog.api.domain.user.service

import com.blog.api.common.exception.CustomException
import com.blog.api.common.exception.ErrorCode
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RefreshTokenService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    companion object {
        private const val REFRESH_TOKEN_PREFIX = "refresh_token:"
        private const val REFRESH_TOKEN_EXPIRE_DAYS = 7L
    }

    fun saveRefreshToken(userId: Long, refreshToken: String) {
        val key = getKey(userId)
        redisTemplate.opsForValue().set(
            key,
            refreshToken,
            REFRESH_TOKEN_EXPIRE_DAYS,
            TimeUnit.DAYS
        )
    }

    fun getRefreshToken(userId: Long): String {
        val key = getKey(userId)
        return redisTemplate.opsForValue().get(key)
            ?: throw CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
    }

    fun validateRefreshToken(userId: Long, refreshToken: String) {
        val storedToken = getRefreshToken(userId)
        check(storedToken == refreshToken) {
            CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
        }
    }

    fun deleteRefreshToken(userId: Long) {
        val key = getKey(userId)
        redisTemplate.delete(key)
    }

    private fun getKey(userId: Long): String {
        return "$REFRESH_TOKEN_PREFIX$userId"
    }
}
