package com.blog.api.core.api.controller.v1

import com.blog.api.core.api.controller.v1.request.LogoutRequest
import com.blog.api.core.api.controller.v1.request.UserSignInRequest
import com.blog.api.core.api.controller.v1.request.UserTokenRefreshRequest
import com.blog.api.core.api.controller.v1.response.TokenResponse
import com.blog.api.core.support.web.annotation.AuthUser
import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.domain.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "User", description = "사용자 인증 및 관리 API")
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    @Operation(summary = "로그인")
    @PostMapping("/login")
    fun login(
        @RequestBody request: UserSignInRequest
    ): ApiResponse<TokenResponse> {
        val tokenPair = userService.login(request.toUserSignIn())
        return ApiResponse.success(TokenResponse.of(tokenPair))
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    fun refreshToken(
        @RequestBody request: UserTokenRefreshRequest
    ): ApiResponse<TokenResponse> {
        val tokenPair = userService.refreshAccessToken(request.toUserTokenRefresh())
        return ApiResponse.success(TokenResponse.of(tokenPair))
    }

    @Operation(summary = "로그아웃", description = "현재 기기에서 로그아웃 (refresh token 무효화)")
    @PostMapping("/logout")
    fun logout(
        @RequestBody request: LogoutRequest
    ): ApiResponse<Any> {
        userService.logout(request.refreshToken)
        return ApiResponse.success()
    }

    @Operation(summary = "전체 로그아웃", description = "모든 기기에서 로그아웃 (모든 refresh token 무효화)")
    @PostMapping("/logout/all")
    fun logoutAll(
        @AuthUser userId: Long
    ): ApiResponse<Any> {
        userService.logoutAll(userId)
        return ApiResponse.success()
    }

}
