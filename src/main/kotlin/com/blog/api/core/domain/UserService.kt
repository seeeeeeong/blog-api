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
) {
    @Transactional
    fun login(userLogin: UserLogin, clientIp: String): UserToken {
        val normalizedEmail = normalizeEmail(userLogin.email)
        val user = findUserByEmail(normalizedEmail)
        validatePassword(userLogin.password, user.password)
        val userId = user.id!!
        val tokenId = userTokenService.create(userId)
        return createUserToken(userId, user.role.name, tokenId)
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String): UserToken {
        val (tokenId, userId, roleName) = jwtProvider.parseRefreshTokenClaims(refreshToken)
        val newTokenId = userTokenService.rotate(tokenId)
        return createUserToken(userId, roleName, newTokenId)
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

    private fun createUserToken(userId: Long, roleName: String, tokenId: String): UserToken {
        return UserToken(
            accessToken = jwtProvider.generateAccessToken(userId, roleName),
            refreshToken = jwtProvider.generateRefreshToken(userId, tokenId, roleName),
        )
    }

    private fun validatePassword(rawPassword: String, encodedPassword: String) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) throw CoreException(ErrorType.INVALID_PASSWORD)
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()
}
