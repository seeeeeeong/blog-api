package com.blog.api.core.domain.image

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.ImagePresignedProperties
import com.blog.api.core.support.properties.S3Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL

class ImageServiceTest {

    @Test
    fun `gif content type 으로 presigned url 을 생성할 수 있다`() {
        val s3Presigner = mock(S3Presigner::class.java)
        val presignedPutObjectRequest = mock(PresignedPutObjectRequest::class.java)
        val service = fixture(s3Presigner = s3Presigner)
        `when`(s3Presigner.presignPutObject(any(PutObjectPresignRequest::class.java))).thenReturn(presignedPutObjectRequest)
        `when`(presignedPutObjectRequest.url()).thenReturn(URL("https://upload.example.com/presigned"))

        val result = service.generatePresignedUrl(
            adminUserId = 7L,
            contentType = "image/gif",
            folder = "blog",
        )

        assertEquals("https://upload.example.com/presigned", result.uploadUrl)
        assertTrue(result.fileUrl.endsWith(".gif"))
        assertTrue(result.key.startsWith("admin/7/blog/"))
        assertEquals(180L, result.expiresInSeconds)
    }

    @Test
    fun `허용되지 않은 content type 은 예외를 던진다`() {
        val service = fixture()

        val exception = assertThrows<CoreException> {
            service.generatePresignedUrl(
                adminUserId = 7L,
                contentType = "image/tiff",
                folder = "blog",
            )
        }

        assertEquals(ErrorType.INVALID_INPUT, exception.errorType)
    }

    private fun fixture(
        s3Presigner: S3Presigner = mock(S3Presigner::class.java),
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
                allowedContentTypes = listOf("image/jpeg", "image/png", "image/webp", "image/gif"),
                allowedFolders = listOf("blog", "profile"),
            ),
        )
    }
}
