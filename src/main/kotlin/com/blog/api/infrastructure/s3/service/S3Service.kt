package com.blog.api.infrastructure.s3.service

import com.blog.api.global.config.AwsProperties
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.util.*

@Service
class S3Service(
    private val s3Presigner: S3Presigner,
    private val awsProperties: AwsProperties
) {
    
    fun generatePresignedUrl(fileName: String, contentType: String): PresignedUrlResponse {
        val uniqueFileName = "${UUID.randomUUID()}-$fileName"
        val objectKey = "images/$uniqueFileName"
        
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(awsProperties.s3.bucket)
            .key(objectKey)
            .contentType(contentType)
            .build()
        
        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .putObjectRequest(putObjectRequest)
            .build()
        
        val presignedRequest = s3Presigner.presignPutObject(presignRequest)
        
        return PresignedUrlResponse(
            uploadUrl = presignedRequest.url().toString(),
            fileUrl = "https://${awsProperties.s3.bucket}.s3.${awsProperties.region}.amazonaws.com/$objectKey",
            key = objectKey
        )
    }
}

data class PresignedUrlResponse(
    val uploadUrl: String,
    val fileUrl: String,
    val key: String
)
