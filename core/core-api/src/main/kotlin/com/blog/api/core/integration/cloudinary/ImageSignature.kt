package com.blog.api.core.integration.cloudinary

data class ImageSignature(
    val signature: String,
    val timestamp: Long,
    val apiKey: String,
    val cloudName: String
)