package com.blog.api.core.domain.postoutbox

import com.blog.api.storage.postoutbox.PostOutboxEntity
import com.blog.api.storage.postoutbox.PostOutboxRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.min

@Service
class PostOutboxRelayService(
    private val postOutboxRepository: PostOutboxRepository,
    private val blogAiSyncClient: BlogAiSyncClient,
) {
    companion object {
        private val log = KotlinLogging.logger {}
        private const val BATCH_SIZE = 20
        private const val BASE_BACKOFF_SECONDS = 30L
        private const val MAX_BACKOFF_SECONDS = 600L
        private const val MAX_BACKOFF_SHIFT = 6
    }

    @Transactional(readOnly = true)
    fun fetchPendingIds(): List<Long> =
        postOutboxRepository
            .findPending(
                now = LocalDateTime.now(),
                pageable = PageRequest.of(0, BATCH_SIZE),
            ).mapNotNull { it.id }

    @Transactional
    fun relayOne(outboxId: Long) {
        val entity = postOutboxRepository.findById(outboxId).orElse(null) ?: return
        if (entity.processedAt != null) return

        try {
            blogAiSyncClient.send(entity.payload)
            entity.markProcessed()
            log.info { "Outbox relayed: id=${entity.id} eventId=${entity.eventId}" }
        } catch (e: Exception) {
            handleFailure(entity, e)
        }
    }

    private fun handleFailure(
        entity: PostOutboxEntity,
        e: Exception,
    ) {
        val nextAttempt = LocalDateTime.now().plusSeconds(nextBackoffSeconds(entity.attempts + 1))
        entity.markFailed(
            error = "${e.javaClass.simpleName}: ${e.message ?: ""}",
            nextAttemptAt = nextAttempt,
        )
        log.warn(e) {
            "Outbox relay failed: id=${entity.id} eventId=${entity.eventId} " +
                "attempts=${entity.attempts} nextAttemptAt=$nextAttempt"
        }
    }

    private fun nextBackoffSeconds(attempts: Int): Long {
        val shift = min(attempts, MAX_BACKOFF_SHIFT)
        val backoff = BASE_BACKOFF_SECONDS shl shift
        return min(backoff, MAX_BACKOFF_SECONDS)
    }
}
