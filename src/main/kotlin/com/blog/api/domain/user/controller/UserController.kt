package com.blog.api.domain.user.controller

import com.blog.api.domain.user.dto.*
import com.blog.api.domain.user.service.UserService
import com.blog.api.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import com.blog.api.common.web.annotation.AuthUser
import org.springframework.web.bind.annotation.*

@Tag(name = "User", description = "사용자 인증 및 관리 API")
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest) =
        ApiResponse.success(userService.login(request))

    @Operation(
        summary = "토큰 갱신",
        description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다."
    )
    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest) =
        ApiResponse.success(userService.refreshAccessToken(request.refreshToken))

    @Operation(
        summary = "내 프로필 조회",
        description = "현재 로그인한 사용자의 프로필 정보를 조회합니다."
    )
    @GetMapping("/me")
    fun getMyProfile(@Parameter(hidden = true) @AuthUser userId: Long) =
        ApiResponse.success(userService.getUserById(userId))

    @Operation(
        summary = "프로필 수정",
        description = "닉네임 및 프로필 이미지를 수정합니다."
    )
    @PutMapping("/me")
    fun updateProfile(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest
    ) = ApiResponse.success(userService.updateProfile(userId, request))

    @Operation(
        summary = "비밀번호 변경",
        description = "현재 비밀번호를 확인하고 새 비밀번호로 변경합니다."
    )
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ApiResponse<Unit> {
        userService.changePassword(userId, request)
        return ApiResponse.success(Unit)
    }
}