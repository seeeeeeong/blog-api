package com.blog.api.infrastructure.s3.dto

data class PresignedUrlResponse(
    val uploadUrl: String,
    val fileUrl: String,
    val key: String
)