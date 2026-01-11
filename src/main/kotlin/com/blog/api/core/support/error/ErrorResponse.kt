package com.blog.api.core.support.error

data class ErrorResponse(
    val status: Int,
    val message: String,
    val errors: List<String>? = null
)
