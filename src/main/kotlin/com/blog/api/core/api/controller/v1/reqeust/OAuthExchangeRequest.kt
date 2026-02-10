package com.blog.api.core.api.controller.v1.reqeust

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType

data class OAuthExchangeRequest(
    val code: String
) {
    fun toCode(): String {
        if (code.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        return code
    }
}
