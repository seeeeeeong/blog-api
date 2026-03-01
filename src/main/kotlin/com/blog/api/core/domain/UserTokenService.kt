package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    @param:Value("\${refresh-token.expiration-seconds:1209600}") private val expirationSeconds: Long,
) {

    fun create(userId: Long): String = issueToken(userId)

    fun rotate(tokenId: String): String {
        val userId = findUserIdByTokenId(tokenId)
        val newTokenId = issueToken(userId)
        refreshTokenRepository.delete(tokenId)
        return newTokenId
    }

    fun revoke(tokenId: String) {
        refreshTokenRepository.delete(tokenId)
    }

    private fun findUserIdByTokenId(tokenId: String): Long {
        return refreshTokenRepository.findUserIdByTokenId(tokenId)
            ?: throw CoreException(ErrorType.REFRESH_TOKEN_NOT_FOUND)
    }

    private fun issueToken(userId: Long): String {
        val tokenId = createTokenId()
        refreshTokenRepository.save(tokenId, userId, expirationSeconds)
        return tokenId
    }

    private fun createTokenId(): String = UUID.randomUUID().toString()
}
