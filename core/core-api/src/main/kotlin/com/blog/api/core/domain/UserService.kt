package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.security.JwtProvider
import com.blog.api.storage.db.core.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val refreshTokenService: RefreshTokenService
) {

    @Transactional
    fun login(userSignIn: UserSignIn): Token {
        val user = userRepository.findByEmail(userSignIn.email)
            ?: throw CoreException(ErrorType.USER_NOT_FOUND)

        if (passwordEncoder.matches(userSignIn.password, user.password) == false) {
            throw CoreException(ErrorType.INVALID_PASSWORD)
        }

        val (tokenId, _) = refreshTokenService.createRefreshToken(user.id!!)

        val accessToken = jwtProvider.generateAccessToken(user.id!!, user.role.name)
        val refreshToken = jwtProvider.generateRefreshToken(user.id!!, tokenId, user.role.name)

        return Token(
            accessToken = accessToken,
            refreshToken = refreshToken,
            refreshTokenId = tokenId,
            user = User(
                id = user.id!!,
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl ?: "",
                role = user.role,
                createdAt = user.createdAt
            )
        )
    }

    @Transactional
    fun refreshAccessToken(userTokenRefresh: UserTokenRefresh): Token {
        val oldTokenId = jwtProvider.getTokenIdFromToken(userTokenRefresh.token)
        val userId = jwtProvider.getUserIdFromToken(userTokenRefresh.token)
        val role = jwtProvider.getRoleFromToken(userTokenRefresh.token)

        val newTokenId = refreshTokenService.rotateRefreshToken(oldTokenId)

        val user = userRepository.findById(userId)
            .orElseThrow { CoreException(ErrorType.USER_NOT_FOUND) }

        val newAccessToken = jwtProvider.generateAccessToken(userId, role)
        val newRefreshToken = jwtProvider.generateRefreshToken(userId, newTokenId, role)

        return Token(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            refreshTokenId = newTokenId,
            user = User(
                id = user.id!!,
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl ?: "",
                role = user.role,
                createdAt = user.createdAt
            )
        )
    }

    @Transactional
    fun logout(refreshToken: String) {
        val tokenId = jwtProvider.getTokenIdFromToken(refreshToken)
        refreshTokenService.revokeToken(tokenId)
    }

    @Transactional
    fun logoutAll(userId: Long) {
        refreshTokenService.revokeAllUserTokens(userId)
    }
}
