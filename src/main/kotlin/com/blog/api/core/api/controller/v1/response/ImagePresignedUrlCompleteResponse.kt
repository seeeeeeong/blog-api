package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.ImageUploadComplete

data class ImagePresignedUrlCompleteResponse(
    val fileUrl: String,
    val key: String,
) {
    companion object {
        fun of(imageUploadComplete: ImageUploadComplete): ImagePresignedUrlCompleteResponse {
            return ImagePresignedUrlCompleteResponse(
                fileUrl = imageUploadComplete.fileUrl,
                key = imageUploadComplete.key
            )
        }
    }
}
