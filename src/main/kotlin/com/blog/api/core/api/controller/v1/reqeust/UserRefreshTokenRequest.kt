package com.blog.api.core.api.controller.v1.reqeust

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType

data class UserRefreshTokenRequest(
    val refreshToken: String
) {
    fun toRefreshToken(): String {
        if (refreshToken.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        return refreshToken
    }
}
