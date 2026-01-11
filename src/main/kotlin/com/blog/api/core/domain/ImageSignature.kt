package com.blog.api.core.domain

data class ImageSignature(
    val signature: String,
    val timestamp: Long,
    val apiKey: String,
    val cloudName: String
)