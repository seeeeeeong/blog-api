package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.security.JwtProvider
import com.blog.api.storage.UserEntity
import com.blog.api.storage.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val userTokenService: UserTokenService,
    private val loginRateLimitService: LoginRateLimitService,
) {
    private data class RefreshTokenClaims(
        val tokenId: String,
        val userId: Long,
        val roleName: String,
    )

    @Transactional
    fun login(userLogin: UserLogin, clientIp: String): UserToken {
        val normalizedEmail = normalizeEmail(userLogin.email)
        loginRateLimitService.checkOrThrow(clientIp, normalizedEmail)

        return try {
            val user = findUserByEmail(normalizedEmail)
            validatePassword(userLogin.password, user.password)
            val userId = user.id!!
            val tokenId = userTokenService.create(userId)
            loginRateLimitService.clearFailures(clientIp, normalizedEmail)
            createUserToken(userId, user.role.name, tokenId)
        } catch (e: CoreException) {
            recordFailureIfNeeded(e.errorType, clientIp, normalizedEmail)
            throw e
        }
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String): UserToken {
        val claims = extractRefreshTokenClaims(refreshToken)
        val newTokenId = userTokenService.rotate(claims.tokenId)
        return createUserToken(claims.userId, claims.roleName, newTokenId)
    }

    @Transactional
    fun logout(refreshToken: String) {
        val tokenId = jwtProvider.getTokenIdFromToken(refreshToken)
        userTokenService.revoke(tokenId)
    }

    private fun findUserByEmail(email: String): UserEntity {
        return userRepository.findByEmail(email)
            ?: throw CoreException(ErrorType.USER_NOT_FOUND)
    }

    private fun extractRefreshTokenClaims(refreshToken: String): RefreshTokenClaims {
        return RefreshTokenClaims(
            tokenId = jwtProvider.getTokenIdFromToken(refreshToken),
            userId = jwtProvider.getUserIdFromToken(refreshToken),
            roleName = jwtProvider.getRoleFromToken(refreshToken),
        )
    }

    private fun createUserToken(userId: Long, roleName: String, tokenId: String): UserToken {
        return UserToken(
            accessToken = jwtProvider.generateAccessToken(userId, roleName),
            refreshToken = jwtProvider.generateRefreshToken(userId, tokenId, roleName),
        )
    }

    private fun validatePassword(rawPassword: String, encodedPassword: String) {
        val isMatched = passwordEncoder.matches(rawPassword, encodedPassword)
        if (isMatched) {
            return
        }
        throw CoreException(ErrorType.INVALID_PASSWORD)
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun recordFailureIfNeeded(errorType: ErrorType, clientIp: String, email: String) {
        val shouldTrackFailure = when (errorType) {
            ErrorType.USER_NOT_FOUND, ErrorType.INVALID_PASSWORD -> true
            else -> false
        }
        if (shouldTrackFailure) {
            loginRateLimitService.recordFailure(clientIp, email)
        }
    }
}
