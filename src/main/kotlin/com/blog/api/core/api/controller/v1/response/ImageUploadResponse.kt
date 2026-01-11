package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.ImageUpload

data class ImageUploadResponse(
    val url: String,
    val publicId: String,
    val format: String,
    val width: Int,
    val height: Int
) {
    companion object {
        fun of(imageUpload: ImageUpload): ImageUploadResponse {
            return ImageUploadResponse(
                url = imageUpload.url,
                publicId = imageUpload.publicId,
                format = imageUpload.format,
                width = imageUpload.width,
                height = imageUpload.height
            )
        }
    }
}
