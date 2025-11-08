package com.blog.api.domain.user.controller

import com.blog.api.domain.user.dto.LoginRequest
import com.blog.api.domain.user.dto.SignupRequest
import com.blog.api.domain.user.dto.TokenResponse
import com.blog.api.domain.user.dto.UserResponse
import com.blog.api.domain.user.service.UserService
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
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signup(request))
    }
    
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        return ResponseEntity.ok(userService.login(request))
    }
    
    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: Long): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.getUserById(userId))
    }
}
