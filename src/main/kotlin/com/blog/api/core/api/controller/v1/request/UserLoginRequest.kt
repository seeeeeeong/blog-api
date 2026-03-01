package com.blog.api.core.api.controller.v1.request

import com.blog.api.core.domain.UserLogin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UserLoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val password: String
) {
    fun toUserLogin(): UserLogin {
        return UserLogin(
            email = email,
            password = password
        )
    }
}
