package com.blog.api.core.domain.post

import com.blog.api.core.domain.postoutbox.EnqueueOutboxCommand
import com.blog.api.core.domain.postoutbox.PostOutboxService
import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.BlogAiProperties
import com.blog.api.storage.category.CategoryRepository
import com.blog.api.storage.post.PostEntity
import com.blog.api.storage.post.PostRepository
import com.blog.api.storage.postoutbox.OutboxEventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.util.Optional

class PostServiceTest {
    @Test
    fun `updatePost 시 PostCacheEvictEvent를 발행한다`() {
        val postRepository = mock<PostRepository>()
        val eventPublisher = mock<ApplicationEventPublisher>()
        val service = fixture(postRepository, eventPublisher = eventPublisher)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val postUpdate =
            PostUpdate(
                categoryId = 1L,
                title = "updated",
                content = "updated content",
                thumbnailUrl = null,
                status = PostStatus.PUBLISHED,
            )
        service.updatePost(1L, 10L, postUpdate)

        verify(eventPublisher).publishEvent(PostCacheEvictEvent(1L))
    }

    @Test
    fun `deletePost 시 PostCacheEvictEvent를 발행한다`() {
        val postRepository = mock<PostRepository>()
        val eventPublisher = mock<ApplicationEventPublisher>()
        val service = fixture(postRepository, eventPublisher = eventPublisher)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

        service.deletePost(1L, 10L)

        verify(eventPublisher).publishEvent(PostCacheEvictEvent(1L))
    }

    @Test
    fun `updatePost 시 소유자가 아니면 FORBIDDEN`() {
        val postRepository = mock<PostRepository>()
        val service = fixture(postRepository)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val postUpdate = PostUpdate(1L, "title", "content", null, PostStatus.PUBLISHED)
        val exception =
            assertThrows(CoreException::class.java) {
                service.updatePost(1L, 99L, postUpdate)
            }

        assertEquals(ErrorType.FORBIDDEN, exception.errorType)
    }

    @Test
    fun `updatePost 발행 상태이면 UPSERT 이벤트를 outbox에 enqueue한다`() {
        val postRepository = mock<PostRepository>()
        val postOutboxService = mock<PostOutboxService>()
        val service = fixture(postRepository = postRepository, postOutboxService = postOutboxService)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

        service.updatePost(
            1L,
            10L,
            PostUpdate(1L, "new", "new content", null, PostStatus.PUBLISHED),
        )

        val captor = argumentCaptor<EnqueueOutboxCommand>()
        verify(postOutboxService).enqueue(captor.capture())
        assertEquals(OutboxEventType.UPSERT, captor.firstValue.eventType)
        assertEquals(1L, captor.firstValue.postId)
    }

    @Test
    fun `updatePost 초안 상태이면 DELETE 이벤트를 outbox에 enqueue한다`() {
        val postRepository = mock<PostRepository>()
        val postOutboxService = mock<PostOutboxService>()
        val service = fixture(postRepository = postRepository, postOutboxService = postOutboxService)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

        service.updatePost(
            1L,
            10L,
            PostUpdate(1L, "new", "new content", null, PostStatus.DRAFT),
        )

        val captor = argumentCaptor<EnqueueOutboxCommand>()
        verify(postOutboxService).enqueue(captor.capture())
        assertEquals(OutboxEventType.DELETE, captor.firstValue.eventType)
    }

    @Test
    fun `deletePost 시 DELETE 이벤트를 outbox에 enqueue한다`() {
        val postRepository = mock<PostRepository>()
        val postOutboxService = mock<PostOutboxService>()
        val service = fixture(postRepository = postRepository, postOutboxService = postOutboxService)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

        service.deletePost(1L, 10L)

        val captor = argumentCaptor<EnqueueOutboxCommand>()
        verify(postOutboxService).enqueue(captor.capture())
        assertEquals(OutboxEventType.DELETE, captor.firstValue.eventType)
        assertEquals(1L, captor.firstValue.postId)
    }

    @Test
    fun `restorePost 이후 초안 상태이면 outbox enqueue하지 않는다`() {
        val postRepository = mock<PostRepository>()
        val postOutboxService = mock<PostOutboxService>()
        val service = fixture(postRepository = postRepository, postOutboxService = postOutboxService)
        val post = post(id = 1L, userId = 10L, status = PostStatus.DELETED)
        whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

        service.restorePost(1L, 10L)

        verifyNoInteractions(postOutboxService)
    }

    private fun fixture(
        postRepository: PostRepository = mock(),
        categoryRepository: CategoryRepository = mock(),
        eventPublisher: ApplicationEventPublisher = mock(),
        postOutboxService: PostOutboxService = mock(),
    ): PostService {
        whenever(categoryRepository.existsById(1L)).thenReturn(true)
        return PostService(
            postRepository = postRepository,
            categoryRepository = categoryRepository,
            eventPublisher = eventPublisher,
            postOutboxService = postOutboxService,
            blogAiProperties =
                BlogAiProperties(
                    baseUrl = "http://localhost:8081",
                    internalKey = "test",
                    webBaseUrl = "http://localhost:5173",
                ),
        )
    }

    private fun post(
        id: Long,
        userId: Long,
        status: PostStatus,
    ) = PostEntity(
        id = id,
        userId = userId,
        categoryId = 1L,
        title = "title-$id",
        content = "content-$id",
        status = status,
    )
}
