package com.blog.api.core.support.error

import org.springframework.boot.logging.LogLevel
import org.springframework.http.HttpStatus

enum class ErrorType(
    val status: HttpStatus,
    val code: String,
    val message: String,
    val logLevel: LogLevel = LogLevel.INFO
) {
    // ==================== User ====================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "User not found", LogLevel.WARN),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "USER_004", "Invalid password"),

    // ==================== Auth & Token ====================
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "Invalid token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "Expired token"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_003", "Unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_004", "Forbidden", LogLevel.WARN),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_005", "Refresh token not found"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_006", "Refresh token expired"),
    OAUTH_STATE_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_007", "Invalid OAuth state", LogLevel.WARN),
    OAUTH_CODE_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_008", "Invalid or expired OAuth code", LogLevel.WARN),
    LOGIN_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_009", "Too many login attempts", LogLevel.WARN),

    // ==================== Post ====================
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_001", "Post not found", LogLevel.WARN),

    // ==================== Comment ====================
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_001", "Comment not found", LogLevel.WARN),

    // ==================== Category ====================
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_001", "Category not found", LogLevel.WARN),

    // ==================== Common ====================
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "Invalid input"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "Internal server error", LogLevel.ERROR),
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "An unexpected error occurred", LogLevel.ERROR)
}
