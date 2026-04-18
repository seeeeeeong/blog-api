package com.blog.api.core.domain.postoutbox

import com.blog.api.storage.postoutbox.PostOutboxEntity
import com.blog.api.storage.postoutbox.PostOutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PostOutboxService(
    private val postOutboxRepository: PostOutboxRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun enqueue(command: EnqueueOutboxCommand) {
        val eventId = UUID.randomUUID()
        val payload = serializePayload(command, eventId)
        val entity =
            PostOutboxEntity(
                eventId = eventId,
                postId = command.postId,
                eventType = command.eventType,
                payload = payload,
            )
        postOutboxRepository.save(entity)
    }

    private fun serializePayload(
        command: EnqueueOutboxCommand,
        eventId: UUID,
    ): String {
        val payload =
            SyncPayload(
                externalId = command.postId.toString(),
                eventType = command.eventType.name,
                sourceUpdatedAt = command.sourceUpdatedAt,
                eventId = eventId.toString(),
                title = command.title,
                content = command.content,
                url = command.url,
                author = command.author,
                publishedAt = command.publishedAt,
            )
        return objectMapper.writeValueAsString(payload)
    }
}
