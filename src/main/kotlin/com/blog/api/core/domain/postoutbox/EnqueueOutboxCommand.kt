package com.blog.api.core.domain.postoutbox

import com.blog.api.storage.postoutbox.OutboxEventType
import java.time.OffsetDateTime

data class EnqueueOutboxCommand(
    val postId: Long,
    val eventType: OutboxEventType,
    val sourceUpdatedAt: OffsetDateTime,
    val title: String? = null,
    val content: String? = null,
    val url: String? = null,
    val author: String? = null,
    val publishedAt: OffsetDateTime? = null,
)
