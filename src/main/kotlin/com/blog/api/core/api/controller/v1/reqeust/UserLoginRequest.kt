package com.blog.api.core.api.controller.v1.reqeust

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.domain.UserLogin

data class UserLoginRequest(
    val email: String,
    val password: String
) {
    fun toUserLogin(): UserLogin {
        if (email.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (emailPattern.matches(email) == false) {
            throw CoreException(ErrorType.INVALID_INPUT)
        }
        if (password.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)

        return UserLogin(
            email = email,
            password = password
        )
    }
}
