package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.ImagePresignedUrl

data class ImagePresignedUrlResponse(
    val uploadUrl: String,
    val fileUrl: String,
    val key: String,
) {
    companion object {
        fun of(presignedUrl: ImagePresignedUrl): ImagePresignedUrlResponse {
            return ImagePresignedUrlResponse(
                uploadUrl = presignedUrl.uploadUrl,
                fileUrl = presignedUrl.fileUrl,
                key = presignedUrl.key,
            )
        }
    }
}
