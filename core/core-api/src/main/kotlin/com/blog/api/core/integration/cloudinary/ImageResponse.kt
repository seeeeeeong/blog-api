package com.blog.api.core.integration.cloudinary

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

        fun of(
            url: String,
            publicId: String,
            format: String,
            width: Int,
            height: Int
        ): ImageUploadResponse {
            return ImageUploadResponse(
                url = url,
                publicId = publicId,
                format = format,
                width = width,
                height = height
            )
        }
    }
}

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

        fun of(
            signature: String,
            timestamp: Long,
            apiKey: String,
            cloudName: String
        ): ImageSignatureResponse {
            return ImageSignatureResponse(
                signature = signature,
                timestamp = timestamp,
                apiKey = apiKey,
                cloudName = cloudName
            )
        }
    }
}
