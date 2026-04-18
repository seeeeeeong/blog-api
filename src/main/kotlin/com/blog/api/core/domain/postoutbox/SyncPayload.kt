package com.blog.api.core.domain.postoutbox

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SyncPayload(
    val externalId: String,
    val eventType: String,
    val sourceUpdatedAt: OffsetDateTime,
    val eventId: String,
    val title: String? = null,
    val content: String? = null,
    val url: String? = null,
    val author: String? = null,
    val publishedAt: OffsetDateTime? = null,
)
