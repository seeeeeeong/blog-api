package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.security.JwtProvider
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
) {
    fun login(userLogin: UserLogin): UserToken {
        val normalizedEmail = normalizeEmail(userLogin.email)
        val user = userRepository.findByEmail(normalizedEmail)
            ?: throw CoreException(ErrorType.USER_NOT_FOUND)
        validatePassword(userLogin.password, user.password)
        return createUserToken(user.id!!, user.role.name)
    }

    fun refreshAccessToken(refreshToken: String): UserToken {
        val claims = jwtProvider.parseRefreshTokenClaims(refreshToken)
        return createUserToken(claims.userId, claims.roleName)
    }

    private fun createUserToken(userId: Long, roleName: String): UserToken {
        return UserToken(
            accessToken = jwtProvider.generateAccessToken(userId, roleName),
            refreshToken = jwtProvider.generateRefreshToken(userId, roleName),
        )
    }

    private fun validatePassword(rawPassword: String, encodedPassword: String) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) throw CoreException(ErrorType.INVALID_PASSWORD)
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()
}
