package com.blog.api.core.support.error

class CoreException(
    val errorType: ErrorType,
    val data: Map<String, Any?>? = null,
    override val message: String = errorType.message,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
