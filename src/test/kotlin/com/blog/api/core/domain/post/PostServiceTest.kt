package com.blog.api.core.domain.post

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.enum.UserRole
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.post.PostEntity
import com.blog.api.storage.post.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationEventPublisher
import java.util.Optional

class PostServiceTest {

    @Test
    fun `published 글 조회 시 쿠키가 없으면 PostViewedEvent를 발행한다`() {
        val postRepository = mock(PostRepository::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)
        val service = fixture(postRepository, eventPublisher = eventPublisher)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val command = PostViewCommand(postId = 1L, hasViewedCookie = false)
        val result = service.getPost(command)

        assertEquals(1L, result.id)
        verify(eventPublisher).publishEvent(PostViewedEvent(1L))
    }

    @Test
    fun `published 글 조회 시 쿠키가 있으면 PostViewedEvent를 발행하지 않는다`() {
        val postRepository = mock(PostRepository::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)
        val service = fixture(postRepository, eventPublisher = eventPublisher)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val command = PostViewCommand(postId = 1L, hasViewedCookie = true)
        val result = service.getPost(command)

        assertEquals(1L, result.id)
        verify(eventPublisher, never()).publishEvent(PostViewedEvent(1L))
    }

    @Test
    fun `draft 글은 작성자 본인 admin 이면 조회 가능하다`() {
        val postRepository = mock(PostRepository::class.java)
        val service = fixture(postRepository)
        val post = post(id = 2L, userId = 10L, status = PostStatus.DRAFT)
        `when`(postRepository.findById(2L)).thenReturn(Optional.of(post))

        val command = PostViewCommand(
            postId = 2L,
            hasViewedCookie = false,
            viewerUserId = 10L,
            viewerRole = UserRole.ADMIN,
        )
        val result = service.getPost(command)

        assertEquals(PostStatus.DRAFT, result.status)
    }

    @Test
    fun `draft 글은 비공개로 유지된다`() {
        val postRepository = mock(PostRepository::class.java)
        val service = fixture(postRepository)
        val post = post(id = 3L, userId = 10L, status = PostStatus.DRAFT)
        `when`(postRepository.findById(3L)).thenReturn(Optional.of(post))

        val command = PostViewCommand(
            postId = 3L,
            hasViewedCookie = false,
            viewerUserId = 99L,
            viewerRole = UserRole.ADMIN,
        )
        val exception = assertThrows(CoreException::class.java) {
            service.getPost(command)
        }

        assertEquals(ErrorType.POST_NOT_FOUND, exception.errorType)
    }

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

    private fun fixture(
        postRepository: PostRepository = mock(PostRepository::class.java),
        eventPublisher: ApplicationEventPublisher = mock(ApplicationEventPublisher::class.java),
    ): PostService {
        return PostService(
            postRepository = postRepository,
            postMarkdownConverter = mock(PostMarkdownConverter::class.java),
            cacheManager = mock(CacheManager::class.java),
            eventPublisher = eventPublisher,
        )
    }

    private fun post(id: Long, userId: Long, status: PostStatus): PostEntity {
        return PostEntity(
            id = id,
            userId = userId,
            categoryId = 1L,
            title = "title-$id",
            content = "content-$id",
            status = status,
        )
    }
}
