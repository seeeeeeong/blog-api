package com.blog.api.domain.user.service

import com.blog.api.common.exception.CustomException
import com.blog.api.common.exception.ErrorCode
import com.blog.api.common.redis.service.RedisBaseService
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RefreshTokenService(
    private val redisBaseService: RedisBaseService
) {

    companion object {
        private const val PREFIX = "refresh_token:"
        private const val EXPIRATION_DAYS = 7L
    }

    fun saveRefreshToken(userId: Long, refreshToken: String) {
        redisBaseService.set("$PREFIX$userId", refreshToken, EXPIRATION_DAYS, TimeUnit.DAYS)
    }

    fun getRefreshToken(userId: Long): String =
        redisBaseService.get("$PREFIX$userId") ?: throw CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)

    fun validateRefreshToken(userId: Long, refreshToken: String) {
        getRefreshToken(userId)
            .takeIf { it == refreshToken }
            ?: throw CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
    }

    fun deleteRefreshToken(userId: Long) {
        redisBaseService.delete("$PREFIX$userId")
    }
}
