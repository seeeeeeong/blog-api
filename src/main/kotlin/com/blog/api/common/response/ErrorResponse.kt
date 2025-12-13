package com.blog.api.common.response

data class ErrorResponse(
    val status: Int,
    val message: String,
    val errors: List<String>? = null
)