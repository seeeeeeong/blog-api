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
    private val refreshTokenService: RefreshTokenService,
) {

    @Transactional
    fun login(userSignIn: UserSignIn): UserToken {
        val user = userRepository.findByEmail(userSignIn.email)
            ?: throw CoreException(ErrorType.USER_NOT_FOUND)

        val passwordMatches = passwordEncoder.matches(userSignIn.password, user.password)
        if (passwordMatches == false) throw CoreException(ErrorType.INVALID_PASSWORD)

        val userId = user.id!!
        val roleName = user.role.name
        val (tokenId, _) = refreshTokenService.createRefreshToken(userId)

        val accessToken = jwtProvider.generateAccessToken(userId, roleName)
        val refreshToken = jwtProvider.generateRefreshToken(userId, tokenId, roleName)

        return UserToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            refreshTokenId = tokenId,
            user = User(
                id = userId,
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl ?: "",
                role = user.role,
                createdAt = user.createdAt,
            ),
        )
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String): UserToken {
        val oldTokenId = jwtProvider.getTokenIdFromToken(refreshToken)
        val userId = jwtProvider.getUserIdFromToken(refreshToken)
        val roleName = jwtProvider.getRoleFromToken(refreshToken)

        val newTokenId = refreshTokenService.rotateRefreshToken(oldTokenId)

        val user = userRepository.findById(userId)
            .orElseThrow { CoreException(ErrorType.USER_NOT_FOUND) }

        val newAccessToken = jwtProvider.generateAccessToken(userId, roleName)
        val newRefreshToken = jwtProvider.generateRefreshToken(userId, newTokenId, roleName)
        return UserToken(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            refreshTokenId = newTokenId,
            user = User(
                id = user.id!!,
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl ?: "",
                role = user.role,
                createdAt = user.createdAt,
            ),
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
