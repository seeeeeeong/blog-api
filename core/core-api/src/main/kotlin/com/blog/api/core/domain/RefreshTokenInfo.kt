package com.blog.api.core.domain

import java.time.Instant

data class RefreshTokenInfo(
    val tokenId: String,
    val userId: Long,
    val familyId: String,
    val deviceId: String? = null,
    val previousTokenId: String? = null,
    val isUsed: Boolean = false,
    val createdAt: Instant,
    val expiresAt: Instant
) {

    fun isExpired(): Boolean {
        return Instant.now().isAfter(expiresAt)
    }


    fun isValid(): Boolean {
        return !isExpired() && !isUsed
    }


    fun markAsUsed(): RefreshTokenInfo {
        return copy(isUsed = true)
    }

    companion object {
        fun create(
            tokenId: String,
            userId: Long,
            familyId: String,
            deviceId: String?,
            expirationSeconds: Long
        ): RefreshTokenInfo {
            val now = Instant.now()
            return RefreshTokenInfo(
                tokenId = tokenId,
                userId = userId,
                familyId = familyId,
                deviceId = deviceId,
                previousTokenId = null,
                isUsed = false,
                createdAt = now,
                expiresAt = now.plusSeconds(expirationSeconds)
            )
        }

        fun createFromRotation(
            newTokenId: String,
            previousTokenInfo: RefreshTokenInfo,
            expirationSeconds: Long
        ): RefreshTokenInfo {
            val now = Instant.now()
            return RefreshTokenInfo(
                tokenId = newTokenId,
                userId = previousTokenInfo.userId,
                familyId = previousTokenInfo.familyId,
                deviceId = previousTokenInfo.deviceId,
                previousTokenId = previousTokenInfo.tokenId,
                isUsed = false,
                createdAt = now,
                expiresAt = now.plusSeconds(expirationSeconds)
            )
        }
    }
}
