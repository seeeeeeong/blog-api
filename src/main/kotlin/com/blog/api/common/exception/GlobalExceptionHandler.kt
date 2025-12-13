package com.blog.api.common.exception

import com.blog.api.common.response.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler(
    private val messageSource: MessageSource
) {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ErrorResponse> {
        val message = messageSource.getMessage(
            e.errorCode.messageKey,
            null,
            LocaleContextHolder.getLocale()
        )

        logger.warn("CustomException: errorCode={}, message={}", e.errorCode, message)

        val errorResponse = ErrorResponse(
            status = e.errorCode.status,
            message = message
        )
        return ResponseEntity.status(e.errorCode.status).body(errorResponse)
    }
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val invalidValueMessage = messageSource.getMessage(
            "validation.invalid.value",
            null,
            LocaleContextHolder.getLocale()
        )

        val errors = e.bindingResult.allErrors
            .map { error ->
                val fieldName = (error as FieldError).field
                val errorMessage = error.defaultMessage ?: invalidValueMessage
                "$fieldName: $errorMessage"
            }

        logger.info("Validation failed: errors={}", errors)

        val message = messageSource.getMessage(
            "validation.failed",
            null,
            LocaleContextHolder.getLocale()
        )

        val errorResponse = ErrorResponse(
            status = 400,
            message = message,
            errors = errors
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred: message={}", e.message, e)

        val message = messageSource.getMessage(
            "error.internal.server.error",
            null,
            LocaleContextHolder.getLocale()
        )

        val errorResponse = ErrorResponse(
            status = 500,
            message = message
        )
        return ResponseEntity.internalServerError().body(errorResponse)
    }
}


