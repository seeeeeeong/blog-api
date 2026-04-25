package com.blog.api.core.domain.post

import com.blog.api.core.support.properties.S3Properties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

@Component
class PostImageCleanupListener(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties,
) {
    companion object {
        private val log = KotlinLogging.logger {}
        private val IMAGE_URL_REGEX = Regex("""!\[.*?]\((https?://\S+?)\)""")
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onPostDeleted(event: PostDeletedEvent) {
        val keys = extractS3Keys(event)
        if (keys.isEmpty()) return

        keys.forEach { key ->
            try {
                s3Client.deleteObject(
                    DeleteObjectRequest
                        .builder()
                        .bucket(s3Properties.bucket)
                        .key(key)
                        .build(),
                )
                log.info { "Deleted S3 object: key=$key, postId=${event.postId}" }
            } catch (e: Exception) {
                log.warn(e) { "Failed to delete S3 object: key=$key, postId=${event.postId}" }
            }
        }
    }

    private fun extractS3Keys(event: PostDeletedEvent): List<String> {
        val keys = mutableListOf<String>()

        val thumbnailKey = event.thumbnailUrl?.let(::extractKeyFromUrl)
        if (thumbnailKey != null) keys.add(thumbnailKey)

        IMAGE_URL_REGEX.findAll(event.content).forEach { match ->
            val key = extractKeyFromUrl(match.groupValues[1]) ?: return@forEach
            keys.add(key)
        }

        return keys.distinct()
    }

    private fun extractKeyFromUrl(url: String): String? {
        val cloudfrontDomain = s3Properties.cloudfrontDomain
        val s3Host = "${s3Properties.bucket}.s3.${s3Properties.region}.amazonaws.com"

        return when {
            cloudfrontDomain.isNotBlank() && url.contains(cloudfrontDomain) -> {
                url.substringAfter("$cloudfrontDomain/")
            }

            url.contains(s3Host) -> {
                url.substringAfter("$s3Host/")
            }

            else -> {
                null
            }
        }
    }
}
