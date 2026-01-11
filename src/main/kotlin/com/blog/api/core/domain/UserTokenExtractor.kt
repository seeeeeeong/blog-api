package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class UserTokenExtractor {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val BEARER_PREFIX_LENGTH = 7
    }

    fun extract(authorizationHeader: String?): String? =
        authorizationHeader
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX_LENGTH)

    fun extractOrThrow(authorizationHeader: String?): String =
        extract(authorizationHeader) ?: throw CoreException(ErrorType.INVALID_TOKEN)
}
