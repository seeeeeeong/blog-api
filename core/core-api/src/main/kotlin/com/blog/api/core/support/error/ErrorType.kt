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
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER_002", "Email already exists"),
    NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER_003", "Nickname already exists"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "USER_004", "Invalid password"),
    OLD_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "USER_005", "Old password mismatch"),

    // ==================== Auth & Token ====================
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "Invalid token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "Expired token"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_003", "Unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_004", "Forbidden", LogLevel.WARN),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_005", "Refresh token not found"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_006", "Refresh token expired"),

    // ==================== Email Verification ====================
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_001", "Email send failed", LogLevel.ERROR),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "EMAIL_002", "Invalid verification code"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "EMAIL_003", "Verification code expired"),

    // ==================== Post ====================
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_001", "Post not found", LogLevel.WARN),

    // ==================== Comment ====================
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_001", "Comment not found", LogLevel.WARN),

    // ==================== Category ====================
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_001", "Category not found", LogLevel.WARN),

    // ==================== Embedding ====================
    EMBEDDING_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMBEDDING_001", "Embedding generation failed", LogLevel.ERROR),
    EMBEDDING_API_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "EMBEDDING_002", "Embedding API quota exceeded", LogLevel.ERROR),

    // ==================== Common ====================
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "Invalid input"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "Internal server error", LogLevel.ERROR),
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "An unexpected error occurred", LogLevel.ERROR)
}
