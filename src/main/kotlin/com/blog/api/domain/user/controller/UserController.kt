package com.blog.api.domain.user.controller

import com.blog.api.domain.user.dto.LoginRequest
import com.blog.api.domain.user.dto.SignupRequest
import com.blog.api.domain.user.service.UserService
import com.blog.api.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "User", description = "사용자 인증 및 관리 API")
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    @Operation(
        summary = "회원가입",
        description = "새로운 사용자를 등록합니다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@Valid @RequestBody request: SignupRequest) =
        ApiResponse.success(userService.signup(request))

    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest) =
        ApiResponse.success(userService.login(request))

    @Operation(
        summary = "사용자 조회",
        description = "사용자 ID로 사용자 정보를 조회합니다."
    )
    @GetMapping("/{userId}")
    fun getUser(@Parameter(description = "사용자 ID") @PathVariable userId: Long) =
        ApiResponse.success(userService.getUserById(userId))
}