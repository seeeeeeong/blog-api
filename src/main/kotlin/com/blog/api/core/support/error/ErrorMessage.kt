package com.blog.api.core.support.error

import org.slf4j.MDC

data class ErrorMessage(
    val code: String,
    val message: String,
    val data: Any? = null,
    val traceId: String? = null,
) {
    constructor(errorType: ErrorType, data: Any? = null, message: String? = null) : this(
        code = errorType.code,
        message = message ?: errorType.message,
        data = data,
        traceId = MDC.get("requestId"),
    )
}
