package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.ImagePresignedProperties
import com.blog.api.core.support.properties.S3Properties
import com.blog.api.storage.ImageUploadTokenRepository
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
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
    private val imageUploadTokenRepository: ImageUploadTokenRepository,
) {
    companion object {
        private const val ROOT_FOLDER_PREFIX = "admin"
    }

    fun uploadImage(adminUserId: Long, bytes: ByteArray, contentType: String, folder: String? = null): ImageUpload {
        val normalizedContentType = normalizeContentType(contentType)
        validateContentType(normalizedContentType)
        val targetFolder = resolveFolder(folder)
        val key = generateKey(adminUserId, targetFolder, normalizedContentType)

        s3Client.putObject(
            createPutObjectRequest(key, normalizedContentType),
            RequestBody.fromBytes(bytes)
        )

        return ImageUpload(
            url = buildFileUrl(key),
            key = key,
            format = findExtension(normalizedContentType),
        )
    }

    fun generatePresignedUrl(adminUserId: Long, contentType: String, folder: String? = null): ImagePresignedUrl {
        val normalizedContentType = normalizeContentType(contentType)
        validateContentType(normalizedContentType)
        val targetFolder = resolveFolder(folder)
        val key = generateKey(adminUserId, targetFolder, normalizedContentType)
        val uploadToken = UUID.randomUUID().toString()

        imageUploadTokenRepository.save(
            uploadToken = uploadToken,
            userId = adminUserId,
            key = key,
            ttlSeconds = imagePresignedProperties.oneTimeTokenTtlSeconds
        )

        val presignedUrl = s3Presigner.presignPutObject(
            createPutObjectPresignRequest(key, normalizedContentType, imagePresignedProperties.ttlSeconds)
        )

        return ImagePresignedUrl(
            uploadUrl = presignedUrl.url().toString(),
            fileUrl = buildFileUrl(key),
            key = key,
            uploadToken = uploadToken,
            expiresInSeconds = imagePresignedProperties.ttlSeconds,
        )
    }

    fun completePresignedUpload(adminUserId: Long, uploadToken: String, key: String): ImageUploadComplete {
        val normalizedUploadToken = uploadToken.trim()
        val normalizedKey = key.trim()
        if (normalizedUploadToken.isEmpty() || normalizedKey.isEmpty()) throw CoreException(ErrorType.INVALID_INPUT)
        if (!isOwnedKey(adminUserId, normalizedKey)) throw CoreException(ErrorType.INVALID_INPUT)
        val consumed = imageUploadTokenRepository.consume(uploadToken = normalizedUploadToken, userId = adminUserId, key = normalizedKey)
        if (!consumed) throw CoreException(ErrorType.INVALID_INPUT)
        return ImageUploadComplete(fileUrl = buildFileUrl(normalizedKey), key = normalizedKey)
    }

    fun deleteImage(adminUserId: Long, key: String) {
        val normalizedKey = key.trim()
        if (!isOwnedKey(adminUserId, normalizedKey)) throw CoreException(ErrorType.FORBIDDEN)
        s3Client.deleteObject(createDeleteObjectRequest(normalizedKey))
    }

    private fun generateKey(adminUserId: Long, folder: String, contentType: String): String {
        val keyPrefix = ownerPrefix(adminUserId)
        val extension = findExtension(contentType)
        return "$keyPrefix$folder/${UUID.randomUUID()}.$extension"
    }

    private fun buildFileUrl(key: String): String {
        val cloudfrontDomain = s3Properties.cloudfrontDomain
        return if (cloudfrontDomain.isBlank()) {
            "https://${s3Properties.bucket}.s3.${s3Properties.region}.amazonaws.com/$key"
        } else {
            "https://$cloudfrontDomain/$key"
        }
    }

    private fun createPutObjectRequest(key: String, contentType: String): PutObjectRequest {
        return PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .contentType(contentType)
            .build()
    }

    private fun createPutObjectPresignRequest(
        key: String,
        contentType: String,
        ttlSeconds: Long,
    ): PutObjectPresignRequest {
        return PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(ttlSeconds))
            .putObjectRequest(createPutObjectRequest(key, contentType))
            .build()
    }

    private fun createDeleteObjectRequest(key: String): DeleteObjectRequest {
        return DeleteObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .build()
    }

    private fun normalizeContentType(contentType: String): String {
        return contentType.trim().lowercase()
    }

    private fun validateContentType(contentType: String) {
        if (contentType !in imagePresignedProperties.allowedContentTypes) {
            throw CoreException(
                errorType = ErrorType.INVALID_INPUT,
                data = mapOf(
                    "field" to "contentType",
                    "value" to contentType,
                    "reason" to "Only browser-displayable image types are supported",
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

    private fun ownerPrefix(adminUserId: Long): String = "$ROOT_FOLDER_PREFIX/$adminUserId/"

    private fun isOwnedKey(adminUserId: Long, key: String): Boolean {
        return key.startsWith(ownerPrefix(adminUserId))
    }

    private fun findExtension(contentType: String): String = contentType.substringAfter("/")
}
