package com.blog.api.domain.user.service

import com.blog.api.domain.user.dto.*
import com.blog.api.domain.user.entity.User
import com.blog.api.domain.user.repository.UserRepository
import com.blog.api.common.exception.CustomException
import com.blog.api.common.exception.ErrorCode
import com.blog.api.common.security.JwtProvider
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
    fun signup(request: SignupRequest): UserResponse {
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            nickname = request.nickname
        )
        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }

    @Transactional
    fun login(request: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        validatePassword(request.password, user.password)

        val accessToken = jwtProvider.generateAccessToken(user.id!!, user.role.name)
        val refreshToken = jwtProvider.generateRefreshToken(user.id!!, user.role.name)

        refreshTokenService.saveRefreshToken(user.id!!, refreshToken)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = UserResponse.from(user)
        )
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String): TokenResponse {
        val userId = jwtProvider.getUserIdFromToken(refreshToken)
        val role = jwtProvider.getRoleFromToken(refreshToken)

        refreshTokenService.validateRefreshToken(userId, refreshToken)

        val user = userRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }

        val newAccessToken = jwtProvider.generateAccessToken(userId, role)
        val newRefreshToken = jwtProvider.generateRefreshToken(userId, role)

        refreshTokenService.saveRefreshToken(userId, newRefreshToken)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            user = UserResponse.from(user)
        )
    }

    fun getUserById(userId: Long): UserResponse {
        return userRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }
            .let { UserResponse.from(it) }
    }

    @Transactional
    fun updateProfile(userId: Long, request: UpdateProfileRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }

        user.apply {
            nickname = request.nickname
            profileImageUrl = request.profileImageUrl
        }

        return UserResponse.from(user)
    }

    @Transactional
    fun changePassword(userId: Long, request: ChangePasswordRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }

        validatePassword(request.currentPassword, user.password)

        user.apply {
            password = passwordEncoder.encode(request.newPassword)
        }
    }

    private fun validatePassword(rawPassword: String, encodedPassword: String) {
        check(passwordEncoder.matches(rawPassword, encodedPassword)) {
            CustomException(ErrorCode.INVALID_PASSWORD)
        }
    }

}