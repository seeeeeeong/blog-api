package com.blog.api.core.domain.image

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.ImagePresignedProperties
import com.blog.api.core.support.properties.S3Properties
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.util.UUID

@Service
class ImageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val s3Properties: S3Properties,
    private val imagePresignedProperties: ImagePresignedProperties,
) {
    companion object {
        private const val ROOT_FOLDER_PREFIX = "admin"
    }

    fun generatePresignedUrl(adminUserId: Long, contentType: String, folder: String? = null): ImagePresignedUrl {
        val normalizedContentType = normalizeContentType(contentType)
        validateContentType(normalizedContentType)
        val targetFolder = resolveFolder(folder)
        val key = generateKey(adminUserId, targetFolder, normalizedContentType)

        val presignedUrl = s3Presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(imagePresignedProperties.ttlSeconds))
                .putObjectRequest(
                    PutObjectRequest.builder()
                        .bucket(s3Properties.bucket)
                        .key(key)
                        .contentType(normalizedContentType)
                        .build()
                )
                .build()
        )

        return ImagePresignedUrl(
            uploadUrl = presignedUrl.url().toString(),
            fileUrl = buildFileUrl(key),
            key = key,
            expiresInSeconds = imagePresignedProperties.ttlSeconds,
        )
    }

    fun deleteImage(adminUserId: Long, key: String) {
        val normalizedKey = key.trim()
        if (!isOwnedKey(adminUserId, normalizedKey)) throw CoreException(ErrorType.FORBIDDEN)
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(normalizedKey)
                .build()
        )
    }

    private fun generateKey(adminUserId: Long, folder: String, contentType: String): String {
        val extension = contentType.substringAfter("/")
        return "$ROOT_FOLDER_PREFIX/$adminUserId/$folder/${UUID.randomUUID()}.$extension"
    }

    private fun buildFileUrl(key: String): String {
        val cloudfrontDomain = s3Properties.cloudfrontDomain
        return if (cloudfrontDomain.isBlank()) {
            "https://${s3Properties.bucket}.s3.${s3Properties.region}.amazonaws.com/$key"
        } else {
            "https://$cloudfrontDomain/$key"
        }
    }

    private fun normalizeContentType(contentType: String): String = contentType.trim().lowercase()

    private fun validateContentType(contentType: String) {
        if (contentType !in imagePresignedProperties.allowedContentTypes) {
            throw CoreException(
                errorType = ErrorType.INVALID_INPUT,
                data = mapOf(
                    "field" to "contentType",
                    "value" to contentType,
                    "allowedContentTypes" to imagePresignedProperties.allowedContentTypes,
                ),
            )
        }
    }

    private fun resolveFolder(folder: String?): String {
        val candidate = folder?.trim()?.lowercase().orEmpty().ifBlank { s3Properties.defaultFolder.lowercase() }
        if (candidate !in imagePresignedProperties.allowedFolders) throw CoreException(ErrorType.INVALID_INPUT)
        return candidate
    }

    private fun isOwnedKey(adminUserId: Long, key: String): Boolean {
        return key.startsWith("$ROOT_FOLDER_PREFIX/$adminUserId/")
    }
}
