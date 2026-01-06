package com.blog.api.core.integration

import com.blog.api.core.api.config.properties.RefreshTokenProperties
import com.blog.api.core.domain.RefreshTokenInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.TimeUnit

@Repository
class RefreshTokenRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val refreshTokenProperties: RefreshTokenProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TOKEN_KEY_PREFIX = "token:"
        private const val FAMILY_CURRENT_KEY_PREFIX = "family:"
        private const val USER_FAMILIES_KEY_PREFIX = "user:"
        private const val USED_TOKEN_KEY_PREFIX = "used:"
        private const val FAMILY_TOKENS_KEY_SUFFIX = ":tokens"
    }


    fun save(tokenInfo: RefreshTokenInfo) {
        try {
            val key = getTokenKey(tokenInfo.tokenId)
            val value = objectMapper.writeValueAsString(tokenInfo)
            val ttl = calculateTtl(tokenInfo.expiresAt)

            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS)

            updateFamilyCurrent(tokenInfo.familyId, tokenInfo.tokenId, ttl)
            addFamilyToUser(tokenInfo.userId, tokenInfo.familyId, ttl)
            addTokenToFamily(tokenInfo.familyId, tokenInfo.tokenId, ttl)

            logger.debug("Saved refresh token: tokenId={}, familyId={}", tokenInfo.tokenId, tokenInfo.familyId)
        } catch (e: Exception) {
            logger.error("Failed to save refresh token: tokenId={}", tokenInfo.tokenId, e)
            throw e
        }
    }

    fun findByTokenId(tokenId: String): RefreshTokenInfo? {
        return try {
            val key = getTokenKey(tokenId)
            val value = redisTemplate.opsForValue().get(key) ?: return null
            objectMapper.readValue<RefreshTokenInfo>(value)
        } catch (e: Exception) {
            logger.error("Failed to find refresh token: tokenId={}", tokenId, e)
            null
        }
    }

    fun markAsUsed(tokenId: String) {
        try {
            val tokenInfo = findByTokenId(tokenId) ?: return
            val updatedInfo = tokenInfo.markAsUsed()
            save(updatedInfo)

            val usedKey = getUsedTokenKey(tokenId)
            redisTemplate.opsForValue().set(
                usedKey,
                Instant.now().toString(),
                refreshTokenProperties.gracePeriodSeconds,
                TimeUnit.SECONDS
            )

            logger.debug("Marked token as used: tokenId={}", tokenId)
        } catch (e: Exception) {
            logger.error("Failed to mark token as used: tokenId={}", tokenId, e)
        }
    }

    fun wasRecentlyUsed(tokenId: String): Boolean {
        val usedKey = getUsedTokenKey(tokenId)
        return redisTemplate.hasKey(usedKey)
    }


    fun deleteByTokenId(tokenId: String) {
        try {
            val tokenInfo = findByTokenId(tokenId)
            deleteTokenData(tokenId)

            if (tokenInfo != null) {
                removeTokenFromFamily(tokenInfo.familyId, tokenId)

                val currentTokenId = getCurrentTokenId(tokenInfo.familyId)
                if (currentTokenId == tokenId) {
                    redisTemplate.delete(getFamilyCurrentKey(tokenInfo.familyId))
                }
            }
            logger.debug("Deleted refresh token: tokenId={}", tokenId)
        } catch (e: Exception) {
            logger.error("Failed to delete refresh token: tokenId={}", tokenId, e)
        }
    }

    fun deleteByFamilyId(familyId: String) {
        try {
            val familyTokensKey = getFamilyTokensKey(familyId)
            val tokenIds = redisTemplate.opsForSet().members(familyTokensKey) ?: emptySet()
            var userId: Long? = null

            tokenIds.forEach { tokenId ->
                val tokenInfo = findByTokenId(tokenId)
                if (userId == null && tokenInfo != null) {
                    userId = tokenInfo.userId
                }
                deleteTokenData(tokenId)
            }

            redisTemplate.delete(getFamilyCurrentKey(familyId))
            redisTemplate.delete(familyTokensKey)

            if (userId != null) {
                redisTemplate.opsForSet().remove(getUserFamiliesKey(userId!!), familyId)
            }

            logger.debug("Deleted token family: familyId={}", familyId)
        } catch (e: Exception) {
            logger.error("Failed to delete token family: familyId={}", familyId, e)
        }
    }

    fun deleteByUserId(userId: Long) {
        try {
            val families = getUserFamilies(userId)
            families.forEach { familyId ->
                deleteByFamilyId(familyId)
            }

            val userKey = getUserFamiliesKey(userId)
            redisTemplate.delete(userKey)

            logger.debug("Deleted all tokens for user: userId={}", userId)
        } catch (e: Exception) {
            logger.error("Failed to delete tokens for user: userId={}", userId, e)
        }
    }

    fun getCurrentTokenId(familyId: String): String? {
        val key = getFamilyCurrentKey(familyId)
        return redisTemplate.opsForValue().get(key)
    }

    fun getUserFamilies(userId: Long): Set<String> {
        return try {
            val key = getUserFamiliesKey(userId)
            redisTemplate.opsForSet().members(key) ?: emptySet()
        } catch (e: Exception) {
            logger.error("Failed to get user families: userId={}", userId, e)
            emptySet()
        }
    }

    fun countUserFamilies(userId: Long): Long {
        val key = getUserFamiliesKey(userId)
        return redisTemplate.opsForSet().size(key) ?: 0
    }

    fun enforceMaxFamilies(userId: Long) {
        val count = countUserFamilies(userId)
        if (count >= refreshTokenProperties.maxFamiliesPerUser) {
            val families = getUserFamilies(userId)
            val oldestFamily = families.firstOrNull() ?: return
            deleteByFamilyId(oldestFamily)

            val userKey = getUserFamiliesKey(userId)
            redisTemplate.opsForSet().remove(userKey, oldestFamily)

            logger.warn("Removed oldest family for user: userId={}, familyId={}", userId, oldestFamily)
        }
    }


    private fun getTokenKey(tokenId: String) = "$TOKEN_KEY_PREFIX$tokenId"

    private fun getFamilyCurrentKey(familyId: String) = "$FAMILY_CURRENT_KEY_PREFIX$familyId:current"

    private fun getFamilyTokensKey(familyId: String) = "$FAMILY_CURRENT_KEY_PREFIX$familyId$FAMILY_TOKENS_KEY_SUFFIX"

    private fun getUserFamiliesKey(userId: Long) = "$USER_FAMILIES_KEY_PREFIX$userId:families"

    private fun getUsedTokenKey(tokenId: String) = "$USED_TOKEN_KEY_PREFIX$tokenId"

    private fun updateFamilyCurrent(familyId: String, tokenId: String, ttl: Long) {
        val key = getFamilyCurrentKey(familyId)
        redisTemplate.opsForValue().set(key, tokenId, ttl, TimeUnit.SECONDS)
    }

    private fun addFamilyToUser(userId: Long, familyId: String, ttl: Long) {
        val key = getUserFamiliesKey(userId)
        redisTemplate.opsForSet().add(key, familyId)
        redisTemplate.expire(key, ttl + 86400, TimeUnit.SECONDS)
    }

    private fun addTokenToFamily(familyId: String, tokenId: String, ttl: Long) {
        val key = getFamilyTokensKey(familyId)
        redisTemplate.opsForSet().add(key, tokenId)
        redisTemplate.expire(key, ttl + 86400, TimeUnit.SECONDS)
    }

    private fun removeTokenFromFamily(familyId: String, tokenId: String) {
        val key = getFamilyTokensKey(familyId)
        redisTemplate.opsForSet().remove(key, tokenId)
    }

    private fun deleteTokenData(tokenId: String) {
        redisTemplate.delete(getTokenKey(tokenId))
        redisTemplate.delete(getUsedTokenKey(tokenId))
    }

    private fun calculateTtl(expiresAt: Instant): Long {
        val now = Instant.now()
        val ttl = expiresAt.epochSecond - now.epochSecond
        return if (ttl > 0) ttl else 0
    }
}
