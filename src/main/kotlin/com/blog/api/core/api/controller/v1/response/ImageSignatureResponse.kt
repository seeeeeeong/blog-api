package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.ImageSignature

data class ImageSignatureResponse(
    val signature: String,
    val timestamp: Long,
    val apiKey: String,
    val cloudName: String
) {
    companion object {
        fun of(imageSignature: ImageSignature): ImageSignatureResponse {
            return ImageSignatureResponse(
                signature = imageSignature.signature,
                timestamp = imageSignature.timestamp,
                apiKey = imageSignature.apiKey,
                cloudName = imageSignature.cloudName
            )
        }
    }
}
