package com.blog.api.core.domain

import com.blog.api.core.api.config.properties.RefreshTokenProperties
import com.blog.api.core.integration.RefreshTokenRepository
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val refreshTokenProperties: RefreshTokenProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)


    fun createRefreshToken(userId: Long, deviceId: String? = null): Pair<String, String> {
        refreshTokenRepository.enforceMaxFamilies(userId)

        val tokenId = UUID.randomUUID().toString()
        val familyId = UUID.randomUUID().toString()

        val tokenInfo = RefreshTokenInfo.create(
            tokenId = tokenId,
            userId = userId,
            familyId = familyId,
            deviceId = deviceId,
            expirationSeconds = refreshTokenProperties.expirationSeconds
        )

        refreshTokenRepository.save(tokenInfo)

        logger.info("Created new refresh token: userId={}, tokenId={}, familyId={}", userId, tokenId, familyId)
        return tokenId to familyId
    }

    fun rotateRefreshToken(tokenId: String): String {
        val currentTokenInfo = refreshTokenRepository.findByTokenId(tokenId)
            ?: throw CoreException(ErrorType.REFRESH_TOKEN_NOT_FOUND)

        if (currentTokenInfo.isExpired()) {
            logger.warn("Attempted to use expired token: tokenId={}", tokenId)
            throw CoreException(ErrorType.REFRESH_TOKEN_EXPIRED)
        }

        if (currentTokenInfo.isUsed) {
            logger.error("SECURITY: Token reuse detected! Invalidating entire family: tokenId={}, familyId={}",
                tokenId, currentTokenInfo.familyId)

            revokeTokenFamily(currentTokenInfo.familyId)

            throw CoreException(ErrorType.REFRESH_TOKEN_NOT_FOUND)
        }

        if (refreshTokenRepository.wasRecentlyUsed(tokenId)) {
            logger.warn("Token used within grace period: tokenId={}", tokenId)
            val currentFamilyTokenId = refreshTokenRepository.getCurrentTokenId(currentTokenInfo.familyId)
            if (currentFamilyTokenId != null && currentFamilyTokenId != tokenId) {
                return currentFamilyTokenId
            }
        }

        refreshTokenRepository.markAsUsed(tokenId)

        val newTokenId = UUID.randomUUID().toString()
        val newTokenInfo = RefreshTokenInfo.createFromRotation(
            newTokenId = newTokenId,
            previousTokenInfo = currentTokenInfo,
            expirationSeconds = refreshTokenProperties.expirationSeconds
        )

        refreshTokenRepository.save(newTokenInfo)

        logger.info("Rotated refresh token: oldTokenId={}, newTokenId={}, familyId={}",
            tokenId, newTokenId, currentTokenInfo.familyId)

        return newTokenId
    }

    fun validateRefreshToken(tokenId: String): RefreshTokenInfo {
        val tokenInfo = refreshTokenRepository.findByTokenId(tokenId)
            ?: throw CoreException(ErrorType.REFRESH_TOKEN_NOT_FOUND)

        if (tokenInfo.isExpired()) {
            logger.warn("Expired token validation attempted: tokenId={}", tokenId)
            throw CoreException(ErrorType.REFRESH_TOKEN_EXPIRED)
        }

        if (tokenInfo.isUsed && !refreshTokenRepository.wasRecentlyUsed(tokenId)) {
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

    fun getTokenInfo(tokenId: String): RefreshTokenInfo? {
        return refreshTokenRepository.findByTokenId(tokenId)
    }
}
