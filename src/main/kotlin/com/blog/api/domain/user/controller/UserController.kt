package com.blog.api.domain.user.controller

import com.blog.api.domain.user.dto.LoginRequest
import com.blog.api.domain.user.dto.SignupRequest
import com.blog.api.domain.user.dto.TokenResponse
import com.blog.api.domain.user.dto.UserResponse
import com.blog.api.domain.user.service.UserService
import com.blog.api.global.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {
    
    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ApiResponse<UserResponse> {
        return ApiResponse.success(userService.signup(request))
    }
    
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ApiResponse<TokenResponse> {
        return ApiResponse.success(userService.login(request))
    }
    
    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: Long): ApiResponse<UserResponse> {
        return ApiResponse.success(userService.getUserById(userId))
    }
}
