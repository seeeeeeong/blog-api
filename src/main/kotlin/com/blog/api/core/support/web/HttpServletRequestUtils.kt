package com.blog.api.core.support.web

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest

object HttpServletRequestUtils {
    private const val AUTHORIZATION_HEADER = "Authorization"
    private const val BEARER_PREFIX = "Bearer "
    private const val CF_CONNECTING_IP_HEADER = "CF-Connecting-IP"
    private const val X_FORWARDED_FOR_HEADER = "X-Forwarded-For"
    private const val X_REAL_IP_HEADER = "X-Real-IP"
    private const val UNKNOWN_IP = "unknown"

    fun extractBearerToken(request: HttpServletRequest): String? {
        val authorization = request.getHeader(AUTHORIZATION_HEADER)
        if (authorization == null) {
            return null
        }

        if (authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            val token = authorization.substringAfter(" ").trim()
            if (token.isBlank()) {
                return null
            }
            return token
        }

        return null
    }

    fun extractBearerTokenOrThrow(request: HttpServletRequest): String {
        val token = extractBearerToken(request)
        if (token != null) {
            return token
        }
        throw CoreException(ErrorType.INVALID_TOKEN)
    }

    fun resolveClientIp(request: HttpServletRequest): String {
        val cfConnectingIp = request.getHeader(CF_CONNECTING_IP_HEADER)?.trim()
        if (cfConnectingIp.hasText()) {
            return cfConnectingIp.orEmpty()
        }

        val forwardedIp = parseForwardedFor(request.getHeader(X_FORWARDED_FOR_HEADER))
        if (forwardedIp != null) {
            return forwardedIp
        }

        val realIp = request.getHeader(X_REAL_IP_HEADER)?.trim()
        if (realIp.hasText()) {
            return realIp.orEmpty()
        }

        val remoteIp = request.remoteAddr?.trim()
        if (remoteIp.hasText()) {
            return remoteIp.orEmpty()
        }

        return UNKNOWN_IP
    }

    private fun parseForwardedFor(headerValue: String?): String? {
        if (headerValue == null) {
            return null
        }

        val forwardedIp = headerValue
            .split(",")
            .firstOrNull()
            ?.trim()

        if (forwardedIp.hasText()) {
            return forwardedIp
        }
        return null
    }

    private fun String?.hasText(): Boolean {
        if (this == null) {
            return false
        }
        return this.isNotBlank()
    }
}
