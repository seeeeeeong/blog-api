package com.blog.api.domain.user.service

import com.blog.api.domain.user.dto.LoginRequest
import com.blog.api.domain.user.dto.SignupRequest
import com.blog.api.domain.user.dto.TokenResponse
import com.blog.api.domain.user.dto.UserResponse
import com.blog.api.domain.user.entity.User
import com.blog.api.domain.user.repository.UserRepository
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import com.blog.api.global.security.JwtProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {

    @Transactional
    fun signup(request: SignupRequest): UserResponse {
        validateSignUp(request)

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
        val user = findUserByEmail(request.email)

        validatePassword(request.password, user.password)

        return TokenResponse(
            accessToken = jwtProvider.generateAccessToken(user.id!!),
            refreshToken = jwtProvider.generateRefreshToken(user.id!!),
            user = UserResponse.from(user)
        )
    }

    fun getUserById(userId: Long): UserResponse {
        return userRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }
            .let { UserResponse.from(it) }
    }

    private fun validateSignUp(request: SignupRequest) {
        userRepository.findByEmail(request.email)
            ?.let { throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS) }

        userRepository.findByNickname(request.nickname)
            ?.let { throw CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS) }
    }

    private fun findUserByEmail(email: String): User {
        return userRepository.findByEmail(email)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
    }

    private fun validatePassword(rawPassword: String, encodedPassword: String) {
        if (passwordEncoder.matches(rawPassword, encodedPassword)) return
        throw CustomException(ErrorCode.INVALID_PASSWORD)
    }
}