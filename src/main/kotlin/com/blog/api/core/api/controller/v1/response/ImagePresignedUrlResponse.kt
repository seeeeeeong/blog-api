package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.image.ImagePresignedUrl

data class ImagePresignedUrlResponse(
    val uploadUrl: String,
    val fileUrl: String,
    val key: String,
    val expiresInSeconds: Long,
) {
    companion object {
        fun of(presignedUrl: ImagePresignedUrl): ImagePresignedUrlResponse {
            return ImagePresignedUrlResponse(
                uploadUrl = presignedUrl.uploadUrl,
                fileUrl = presignedUrl.fileUrl,
                key = presignedUrl.key,
                expiresInSeconds = presignedUrl.expiresInSeconds,
            )
        }
    }
}
