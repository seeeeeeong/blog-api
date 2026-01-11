package com.blog.api.core.domain

import com.blog.api.core.support.properties.RefreshTokenProperties
import com.blog.api.storage.RefreshTokenRepository
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val refreshTokenProperties: RefreshTokenProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun createRefreshToken(userId: Long): Pair<String, String> {
        refreshTokenRepository.enforceMaxFamilies(userId)

        val tokenId = UUID.randomUUID().toString()
        val familyId = UUID.randomUUID().toString()

        val expiresAt = Instant.now().plusSeconds(refreshTokenProperties.expirationSeconds)
        val tokenInfo = RefreshToken(
            tokenId = tokenId,
            userId = userId,
            familyId = familyId,
            isUsed = false,
            expiresAt = expiresAt,
        )

        refreshTokenRepository.save(tokenInfo)

        logger.info("Created new refresh token: userId={}, tokenId={}, familyId={}", userId, tokenId, familyId)
        return tokenId to familyId
    }

    fun rotateRefreshToken(tokenId: String): String {
        val currentTokenInfo = refreshTokenRepository.findByTokenId(tokenId)
            ?: throw CoreException(ErrorType.REFRESH_TOKEN_NOT_FOUND)

        val expired = Instant.now().isAfter(currentTokenInfo.expiresAt)
        if (expired) {
            logger.warn("Attempted to use expired token: tokenId={}", tokenId)
            throw CoreException(ErrorType.REFRESH_TOKEN_EXPIRED)
        }

        if (currentTokenInfo.isUsed) {
            logger.error(
                "SECURITY: Token reuse detected! Invalidating entire family: tokenId={}, familyId={}",
                tokenId,
                currentTokenInfo.familyId
            )

            revokeTokenFamily(currentTokenInfo.familyId)

            throw CoreException(ErrorType.REFRESH_TOKEN_NOT_FOUND)
        }

        if (refreshTokenRepository.wasRecentlyUsed(tokenId)) {
            logger.warn("Token used within grace period: tokenId={}", tokenId)
            val currentFamilyTokenId = refreshTokenRepository.getCurrentTokenId(currentTokenInfo.familyId)
            when (currentFamilyTokenId) {
                null -> Unit
                tokenId -> Unit
                else -> return currentFamilyTokenId
            }
        }

        refreshTokenRepository.markAsUsed(tokenId)

        val newTokenId = UUID.randomUUID().toString()
        val newExpiresAt = Instant.now().plusSeconds(refreshTokenProperties.expirationSeconds)
        val newTokenInfo = RefreshToken(
            tokenId = newTokenId,
            userId = currentTokenInfo.userId,
            familyId = currentTokenInfo.familyId,
            isUsed = false,
            expiresAt = newExpiresAt,
        )

        refreshTokenRepository.save(newTokenInfo)

        logger.info(
            "Rotated refresh token: oldTokenId={}, newTokenId={}, familyId={}",
            tokenId,
            newTokenId,
            currentTokenInfo.familyId
        )

        return newTokenId
    }

    fun validateRefreshToken(tokenId: String): RefreshToken {
        val tokenInfo = refreshTokenRepository.findByTokenId(tokenId)
            ?: throw CoreException(ErrorType.REFRESH_TOKEN_NOT_FOUND)

        val isExpired = Instant.now().isAfter(tokenInfo.expiresAt)
        if (isExpired) {
            logger.warn("Expired token validation attempted: tokenId={}", tokenId)
            throw CoreException(ErrorType.REFRESH_TOKEN_EXPIRED)
        }

        val usedRecently = refreshTokenRepository.wasRecentlyUsed(tokenId)
        if (tokenInfo.isUsed && usedRecently == false) {
            logger.warn("Used token validation attempted: tokenId={}", tokenId)
            throw CoreException(ErrorType.REFRESH_TOKEN_NOT_FOUND)
        }

        return tokenInfo
    }

    fun revokeToken(tokenId: String) {
        refreshTokenRepository.deleteByTokenId(tokenId)
        logger.info("Revoked refresh token: tokenId={}", tokenId)
    }

    fun revokeTokenFamily(familyId: String) {
        refreshTokenRepository.deleteByFamilyId(familyId)
        logger.info("Revoked token family: familyId={}", familyId)
    }

    fun revokeAllUserTokens(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
        logger.info("Revoked all tokens for user: userId={}", userId)
    }

    fun getUserTokenFamilies(userId: Long): Set<String> {
        return refreshTokenRepository.getUserFamilies(userId)
    }

    fun getTokenInfo(tokenId: String): RefreshToken? {
        return refreshTokenRepository.findByTokenId(tokenId)
    }

}
