package com.blog.api.core.domain.post

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.post.PostEntity
import com.blog.api.storage.post.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.util.Optional

class PostServiceTest {

    @Test
    fun `updatePost 시 PostCacheEvictEvent를 발행한다`() {
        val postRepository = mock(PostRepository::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)
        val service = fixture(postRepository, eventPublisher = eventPublisher)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val postUpdate = PostUpdate(
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
        val postRepository = mock(PostRepository::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)
        val service = fixture(postRepository, eventPublisher = eventPublisher)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(post))

        service.deletePost(1L, 10L)

        verify(eventPublisher).publishEvent(PostCacheEvictEvent(1L))
    }

    @Test
    fun `updatePost 시 소유자가 아니면 FORBIDDEN`() {
        val postRepository = mock(PostRepository::class.java)
        val service = fixture(postRepository)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val postUpdate = PostUpdate(1L, "title", "content", null, PostStatus.PUBLISHED)
        val exception = assertThrows(CoreException::class.java) {
            service.updatePost(1L, 99L, postUpdate)
        }

        assertEquals(ErrorType.FORBIDDEN, exception.errorType)
    }

    private fun fixture(
        postRepository: PostRepository = mock(PostRepository::class.java),
        eventPublisher: ApplicationEventPublisher = mock(ApplicationEventPublisher::class.java),
    ) = PostService(
        postRepository = postRepository,
        eventPublisher = eventPublisher,
    )

    private fun post(id: Long, userId: Long, status: PostStatus) = PostEntity(
        id = id,
        userId = userId,
        categoryId = 1L,
        title = "title-$id",
        content = "content-$id",
        status = status,
    )
}
