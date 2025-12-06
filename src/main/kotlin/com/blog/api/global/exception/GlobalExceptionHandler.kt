package com.blog.api.global.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ErrorResponse> {
        logger.warn("CustomException: errorCode={}, message={}", e.errorCode, e.errorCode.message)

        val errorResponse = ErrorResponse(
            status = e.errorCode.status,
            message = e.errorCode.message
        )
        return ResponseEntity.status(e.errorCode.status).body(errorResponse)
    }
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.allErrors
            .map { error ->
                val fieldName = (error as FieldError).field
                val errorMessage = error.defaultMessage ?: "유효하지 않은 값입니다"
                "$fieldName: $errorMessage"
            }

        logger.info("Validation failed: errors={}", errors)

        val errorResponse = ErrorResponse(
            status = 400,
            message = "입력값 검증 실패",
            errors = errors
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred: message={}", e.message, e)

        val errorResponse = ErrorResponse(
            status = 500,
            message = "서버 오류가 발생했습니다"
        )
        return ResponseEntity.internalServerError().body(errorResponse)
    }
}

data class ErrorResponse(
    val status: Int,
    val message: String,
    val errors: List<String>? = null
)
