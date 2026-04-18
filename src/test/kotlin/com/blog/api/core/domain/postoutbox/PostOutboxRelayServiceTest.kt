package com.blog.api.core.domain.postoutbox

import com.blog.api.storage.postoutbox.OutboxEventType
import com.blog.api.storage.postoutbox.PostOutboxEntity
import com.blog.api.storage.postoutbox.PostOutboxRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class PostOutboxRelayServiceTest {
    @Test
    fun `relayOne 성공 시 processedAt이 기록된다`() {
        val repository = mock<PostOutboxRepository>()
        val client = mock<BlogAiSyncClient>()
        val service = PostOutboxRelayService(repository, client)
        val entity = entity(id = 1L)
        whenever(repository.findById(1L)).thenReturn(Optional.of(entity))

        service.relayOne(1L)

        assertNotNull(entity.processedAt)
        assertNull(entity.lastError)
    }

    @Test
    fun `relayOne 실패 시 attempts가 증가하고 nextAttemptAt이 미래로 설정된다`() {
        val repository = mock<PostOutboxRepository>()
        val client = mock<BlogAiSyncClient>()
        val service = PostOutboxRelayService(repository, client)
        val entity = entity(id = 1L)
        whenever(repository.findById(1L)).thenReturn(Optional.of(entity))
        doThrow(RuntimeException("network")).whenever(client).send(entity.payload)

        service.relayOne(1L)

        assertNull(entity.processedAt)
        assertEquals(1, entity.attempts)
        assertTrue(entity.nextAttemptAt.isAfter(LocalDateTime.now()))
        assertNotNull(entity.lastError)
    }

    @Test
    fun `이미 처리된 이벤트는 다시 전송하지 않는다`() {
        val repository = mock<PostOutboxRepository>()
        val client = mock<BlogAiSyncClient>()
        val service = PostOutboxRelayService(repository, client)
        val entity = entity(id = 1L).also { it.markProcessed() }
        whenever(repository.findById(1L)).thenReturn(Optional.of(entity))

        service.relayOne(1L)

        verifyNoInteractions(client)
    }

    private fun entity(id: Long) =
        PostOutboxEntity(
            id = id,
            eventId = UUID.randomUUID(),
            postId = 42L,
            eventType = OutboxEventType.UPSERT,
            payload = "{}",
        )
}
