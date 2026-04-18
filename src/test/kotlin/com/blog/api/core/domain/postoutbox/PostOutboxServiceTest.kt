package com.blog.api.core.domain.postoutbox

import com.blog.api.storage.postoutbox.OutboxEventType
import com.blog.api.storage.postoutbox.PostOutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PostOutboxServiceTest {
    private val objectMapper: ObjectMapper =
        ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())

    @Test
    fun `enqueue 시 payload가 SyncPayload 구조로 직렬화된다`() {
        val repository = mock<PostOutboxRepository>()
        val service = PostOutboxService(repository, objectMapper)

        service.enqueue(
            EnqueueOutboxCommand(
                postId = 42L,
                eventType = OutboxEventType.UPSERT,
                sourceUpdatedAt = OffsetDateTime.of(2026, 4, 18, 0, 0, 0, 0, ZoneOffset.UTC),
                title = "제목",
                content = "본문",
                url = "https://seeeeeeong.com/posts/42",
                publishedAt = OffsetDateTime.of(2026, 4, 17, 0, 0, 0, 0, ZoneOffset.UTC),
            ),
        )

        val captor = argumentCaptor<com.blog.api.storage.postoutbox.PostOutboxEntity>()
        verify(repository).save(captor.capture())
        val entity = captor.firstValue
        assertEquals(42L, entity.postId)
        assertEquals(OutboxEventType.UPSERT, entity.eventType)
        assertNotNull(entity.eventId)

        val node = objectMapper.readTree(entity.payload)
        assertEquals("42", node.get("externalId").asText())
        assertEquals("UPSERT", node.get("eventType").asText())
        assertEquals("제목", node.get("title").asText())
        assertEquals(entity.eventId.toString(), node.get("eventId").asText())
    }

    @Test
    fun `DELETE 이벤트는 title, content가 생략된다`() {
        val repository = mock<PostOutboxRepository>()
        val service = PostOutboxService(repository, objectMapper)

        service.enqueue(
            EnqueueOutboxCommand(
                postId = 42L,
                eventType = OutboxEventType.DELETE,
                sourceUpdatedAt = OffsetDateTime.of(2026, 4, 18, 0, 0, 0, 0, ZoneOffset.UTC),
            ),
        )

        val captor = argumentCaptor<com.blog.api.storage.postoutbox.PostOutboxEntity>()
        verify(repository).save(captor.capture())
        val node = objectMapper.readTree(captor.firstValue.payload)
        assertEquals("DELETE", node.get("eventType").asText())
        assertEquals(false, node.has("title"))
        assertEquals(false, node.has("content"))
    }
}
