package com.blog.api.core.domain

import com.blog.api.core.support.properties.S3Properties
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
) {

    fun uploadImage(bytes: ByteArray, contentType: String, folder: String? = null): ImageUpload {
        val targetFolder = folder ?: s3Properties.defaultFolder
        val extension = contentType.substringAfter("/")
        val key = "$targetFolder/${UUID.randomUUID()}.$extension"

        val request = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .contentType(contentType)
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(bytes))

        val url = if (s3Properties.cloudfrontDomain.isNotBlank()) {
            "https://${s3Properties.cloudfrontDomain}/$key"
        } else {
            "https://${s3Properties.bucket}.s3.${s3Properties.region}.amazonaws.com/$key"
        }

        return ImageUpload(
            url = url,
            key = key,
            format = extension,
        )
    }

    fun deleteImage(key: String): Boolean {
        val request = DeleteObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .build()

        s3Client.deleteObject(request)
        return true
    }

    fun generatePresignedUrl(contentType: String, folder: String? = null): ImagePresignedUrl {
        val targetFolder = folder ?: s3Properties.defaultFolder
        val extension = contentType.substringAfter("/")
        val key = "$targetFolder/${UUID.randomUUID()}.$extension"

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .putObjectRequest(putObjectRequest)
            .build()

        val presignedUrl = s3Presigner.presignPutObject(presignRequest)

        val fileUrl = if (s3Properties.cloudfrontDomain.isNotBlank()) {
            "https://${s3Properties.cloudfrontDomain}/$key"
        } else {
            "https://${s3Properties.bucket}.s3.${s3Properties.region}.amazonaws.com/$key"
        }

        return ImagePresignedUrl(
            uploadUrl = presignedUrl.url().toString(),
            fileUrl = fileUrl,
            key = key,
        )
    }
}
