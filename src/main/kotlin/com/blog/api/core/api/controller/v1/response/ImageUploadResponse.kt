package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.ImageUpload

data class ImageUploadResponse(
    val url: String,
    val key: String,
    val format: String,
) {
    companion object {
        fun of(imageUpload: ImageUpload): ImageUploadResponse {
            return ImageUploadResponse(
                url = imageUpload.url,
                key = imageUpload.key,
                format = imageUpload.format,
            )
        }
    }
}
