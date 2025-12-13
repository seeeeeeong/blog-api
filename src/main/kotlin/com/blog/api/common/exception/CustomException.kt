package com.blog.api.common.exception

class CustomException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.messageKey)

enum class ErrorCode(
    val status: Int,
    val messageKey: String
) {
    // ==================== User ====================
    USER_NOT_FOUND(404, "error.user.not.found"),
    EMAIL_ALREADY_EXISTS(400, "error.email.already.exists"),
    NICKNAME_ALREADY_EXISTS(400, "error.nickname.already.exists"),
    INVALID_PASSWORD(401, "error.invalid.password"),
    OLD_PASSWORD_MISMATCH(400, "error.old.password.mismatch"),

    // ==================== Auth & Token ====================
    INVALID_TOKEN(401, "error.invalid.token"),
    EXPIRED_TOKEN(401, "error.expired.token"),
    UNAUTHORIZED(401, "error.unauthorized"),
    FORBIDDEN(403, "error.forbidden"),
    REFRESH_TOKEN_NOT_FOUND(404, "error.refresh.token.not.found"),
    REFRESH_TOKEN_EXPIRED(401, "error.refresh.token.expired"),

    // ==================== Email Verification ====================
    EMAIL_SEND_FAILED(500, "error.email.send.failed"),
    INVALID_VERIFICATION_CODE(400, "error.invalid.verification.code"),
    VERIFICATION_CODE_EXPIRED(400, "error.verification.code.expired"),

    // ==================== Post ====================
    POST_NOT_FOUND(404, "error.post.not.found"),

    // ==================== Comment ====================
    COMMENT_NOT_FOUND(404, "error.comment.not.found"),

    // ==================== Category ====================
    CATEGORY_NOT_FOUND(404, "error.category.not.found"),

    // ==================== Common ====================
    INVALID_INPUT(400, "error.invalid.input"),
    INTERNAL_SERVER_ERROR(500, "error.internal.server.error")
}
