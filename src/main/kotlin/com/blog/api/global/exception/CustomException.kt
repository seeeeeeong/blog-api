package com.blog.api.global.exception

class CustomException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)

enum class ErrorCode(
    val status: Int,
    val message: String
) {

    // User
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다"),
    EMAIL_ALREADY_EXISTS(400, "이미 사용중인 이메일입니다"),
    NICKNAME_ALREADY_EXISTS(400, "이미 사용중인 닉네임입니다"),
    INVALID_PASSWORD(401, "비밀번호가 일치하지 않습니다"),
    
    // Auth
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(401, "만료된 토큰입니다"),
    UNAUTHORIZED(401, "인증이 필요합니다"),
    
    // Common
    INVALID_INPUT(400, "잘못된 입력값입니다"),
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다")
}
