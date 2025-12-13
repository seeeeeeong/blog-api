package com.blog.api.infrastructure.cloudinary.dto

data class CloudinaryUploadResponse(
    val url: String,
    val publicId: String,
    val format: String,
    val width: Int,
    val height: Int
)

data class CloudinarySignatureResponse(
    val signature: String,
    val timestamp: Long,
    val apiKey: String,
    val cloudName: String
)
