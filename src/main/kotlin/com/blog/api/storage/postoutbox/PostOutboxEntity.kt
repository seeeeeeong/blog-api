package com.blog.api.storage.postoutbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "post_outbox",
    indexes = [
        Index(name = "idx_post_outbox_post_id", columnList = "post_id"),
    ],
)
class PostOutboxEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: UUID,
    @Column(name = "post_id", nullable = false)
    val postId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    val eventType: OutboxEventType,
    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,
    attempts: Int = 0,
    lastError: String? = null,
    nextAttemptAt: LocalDateTime = LocalDateTime.now(),
    processedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    @Column(nullable = false)
    var attempts: Int = attempts
        protected set

    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = lastError
        protected set

    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: LocalDateTime = nextAttemptAt
        protected set

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = processedAt
        protected set

    fun markProcessed(now: LocalDateTime = LocalDateTime.now()) {
        this.processedAt = now
        this.lastError = null
    }

    fun markFailed(
        error: String,
        nextAttemptAt: LocalDateTime,
    ) {
        this.attempts += 1
        this.lastError = error.take(MAX_ERROR_LEN)
        this.nextAttemptAt = nextAttemptAt
    }

    companion object {
        private const val MAX_ERROR_LEN = 2000

        fun create(
            postId: Long,
            eventType: OutboxEventType,
            payload: String,
        ): PostOutboxEntity =
            PostOutboxEntity(
                eventId = UUID.randomUUID(),
                postId = postId,
                eventType = eventType,
                payload = payload,
            )
    }
}
