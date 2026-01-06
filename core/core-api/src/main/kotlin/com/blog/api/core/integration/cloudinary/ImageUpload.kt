package com.blog.api.core.integration.cloudinary

data class ImageUpload(
    val url: String,
    val publicId: String,
    val format: String,
    val width: Int,
    val height: Int
)


