package com.blog.api.core.api.controller.v1

import com.blog.api.core.api.controller.v1.request.UserLoginRequest
import com.blog.api.core.api.controller.v1.request.UserTokenRequest
import com.blog.api.core.api.controller.v1.response.UserTokenResponse
import com.blog.api.core.domain.user.UserService
import com.blog.api.core.support.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: UserLoginRequest,
    ): ApiResponse<UserTokenResponse> {
        val userToken = userService.login(request.toUserLogin())
        return ApiResponse.success(UserTokenResponse.of(userToken))
    }

    @PostMapping("/refresh")
    fun refreshToken(
        @RequestBody request: UserTokenRequest,
    ): ApiResponse<UserTokenResponse> {
        return ApiResponse.success(UserTokenResponse.of(userService.refreshAccessToken(request.refreshToken)))
    }
}
