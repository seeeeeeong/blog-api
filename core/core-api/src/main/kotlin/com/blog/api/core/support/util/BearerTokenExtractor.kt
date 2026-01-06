package com.blog.api.core.support.util

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class BearerTokenExtractor {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val BEARER_PREFIX_LENGTH = 7
    }

    /**
     * Extract Bearer token from Authorization header
     * Returns null if header is null or doesn't start with "Bearer "
     */
    fun extract(authorizationHeader: String?): String? {
        return authorizationHeader
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX_LENGTH)
    }

    /**
     * Extract Bearer token from Authorization header
     * Throws CoreException if header is invalid
     */
    fun extractOrThrow(authorizationHeader: String?): String {
        return extract(authorizationHeader)
            ?: throw CoreException(ErrorType.INVALID_TOKEN)
    }
}
