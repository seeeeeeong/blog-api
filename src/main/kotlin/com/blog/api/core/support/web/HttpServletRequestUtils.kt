package com.blog.api.core.support.web

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest

object HttpServletRequestUtils {
    private const val AUTHORIZATION_HEADER = "Authorization"
    private const val BEARER_PREFIX = "Bearer "
    private const val X_FORWARDED_FOR_HEADER = "X-Forwarded-For"
    private const val X_REAL_IP_HEADER = "X-Real-IP"
    private const val UNKNOWN_IP = "unknown"

    fun getCookieValue(request: HttpServletRequest, name: String): String? {
        return request.cookies?.firstOrNull { it.name == name }?.value
    }

    fun extractBearerToken(request: HttpServletRequest): String? {
        val authorization = request.getHeader(AUTHORIZATION_HEADER) ?: return null
        if (authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            val token = authorization.substringAfter(" ").trim()
            return token.ifBlank { null }
        }
        return null
    }

    fun extractBearerTokenOrThrow(request: HttpServletRequest): String {
        return extractBearerToken(request) ?: throw CoreException(ErrorType.INVALID_TOKEN)
    }

    fun resolveClientIp(request: HttpServletRequest): String {
        val forwardedIp = request.getHeader(X_FORWARDED_FOR_HEADER)
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (forwardedIp != null) return forwardedIp

        val realIp = request.getHeader(X_REAL_IP_HEADER)?.trim()?.takeIf { it.isNotBlank() }
        if (realIp != null) return realIp

        return request.remoteAddr?.trim()?.takeIf { it.isNotBlank() } ?: UNKNOWN_IP
    }
}
