package com.blog.api.core.api.controller.v1.request

import com.blog.api.core.domain.UserSignIn
import com.blog.api.core.domain.UserTokenRefresh
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType

data class UserSignInRequest(
    val email: String,
    val password: String
) {
    fun toUserSignIn(): UserSignIn {
        if (email.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        if (emailPattern.matches(email) == false) {
            throw CoreException(ErrorType.INVALID_INPUT)
        }
        if (password.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)

        return UserSignIn(
            email = email,
            password = password
        )
    }
}

data class UserTokenRefreshRequest(
    val refreshToken: String
) {
    fun toUserTokenRefresh(): UserTokenRefresh {
        if (refreshToken.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        return UserTokenRefresh(token = refreshToken)
    }
}
