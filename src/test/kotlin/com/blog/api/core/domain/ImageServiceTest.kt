package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.ImagePresignedProperties
import com.blog.api.core.support.properties.S3Properties
import com.blog.api.storage.ImageUploadTokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL

class ImageServiceTest {

    @Test
    fun `tiff content type 으로 presigned url 을 생성할 수 있다`() {
        val s3Presigner = mock(S3Presigner::class.java)
        val imageUploadTokenRepository = mock(ImageUploadTokenRepository::class.java)
        val presignedPutObjectRequest = mock(PresignedPutObjectRequest::class.java)
        val service = fixture(
            s3Presigner = s3Presigner,
            imageUploadTokenRepository = imageUploadTokenRepository,
        )
        `when`(s3Presigner.presignPutObject(any(PutObjectPresignRequest::class.java))).thenReturn(presignedPutObjectRequest)
        `when`(presignedPutObjectRequest.url()).thenReturn(URL("https://upload.example.com/presigned"))

        val result = service.generatePresignedUrl(
            adminUserId = 7L,
            contentType = "image/tiff",
            folder = "blog",
        )

        assertEquals("https://upload.example.com/presigned", result.uploadUrl)
        assertTrue(result.fileUrl.endsWith(".tiff"))
        assertTrue(result.key.startsWith("admin/7/blog/"))
        assertEquals(180L, result.expiresInSeconds)
        verify(imageUploadTokenRepository).save(
            result.uploadToken,
            7L,
            result.key,
            300L,
        )
    }

    @Test
    fun `허용되지 않은 content type 은 예외를 던진다`() {
        val service = fixture()

        val exception = assertThrows<CoreException> {
            service.generatePresignedUrl(
                adminUserId = 7L,
                contentType = "image/bmp",
                folder = "blog",
            )
        }

        assertEquals(ErrorType.INVALID_INPUT, exception.errorType)
    }

    private fun fixture(
        s3Presigner: S3Presigner = mock(S3Presigner::class.java),
        imageUploadTokenRepository: ImageUploadTokenRepository = mock(ImageUploadTokenRepository::class.java),
    ): ImageService {
        return ImageService(
            s3Client = mock(S3Client::class.java),
            s3Presigner = s3Presigner,
            s3Properties = S3Properties(
                bucket = "blog-images-local",
                region = "ap-northeast-2",
                cloudfrontDomain = "",
                defaultFolder = "blog",
            ),
            imagePresignedProperties = ImagePresignedProperties(
                ttlSeconds = 180,
                oneTimeTokenTtlSeconds = 300,
                allowedContentTypes = listOf("image/jpeg", "image/png", "image/webp", "image/gif", "image/tiff"),
                allowedFolders = listOf("blog", "profile"),
            ),
            imageUploadTokenRepository = imageUploadTokenRepository,
        )
    }
}
