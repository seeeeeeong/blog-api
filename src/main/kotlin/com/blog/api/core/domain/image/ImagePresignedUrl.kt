package com.blog.api.core.domain.image

data class ImagePresignedUrl(
    val uploadUrl: String,
    val fileUrl: String,
    val key: String,
    val expiresInSeconds: Long,
)
