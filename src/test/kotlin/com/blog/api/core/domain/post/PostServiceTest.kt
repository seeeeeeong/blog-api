package com.blog.api.core.domain.post

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.enum.UserRole
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.post.PostEntity
import com.blog.api.storage.post.PostRepository
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.cache.CacheManager
import java.util.Optional

class PostServiceTest {

    @Test
    fun `published 글 조회 시 쿠키가 없으면 조회수를 증가시킨다`() {
        val postRepository = mock(PostRepository::class.java)
        val postViewCountUpdater = mock(PostViewCountUpdater::class.java)
        val response = mock(HttpServletResponse::class.java)
        val service = fixture(postRepository, postViewCountUpdater = postViewCountUpdater)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val command = PostViewCommand(postId = 1L, hasViewedCookie = false, response = response)
        val result = service.getPost(command)

        assertEquals(1L, result.id)
        verify(postViewCountUpdater).increment(1L)
    }

    @Test
    fun `published 글 조회 시 쿠키가 있으면 조회수를 증가시키지 않는다`() {
        val postRepository = mock(PostRepository::class.java)
        val postViewCountUpdater = mock(PostViewCountUpdater::class.java)
        val response = mock(HttpServletResponse::class.java)
        val service = fixture(postRepository, postViewCountUpdater = postViewCountUpdater)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val command = PostViewCommand(postId = 1L, hasViewedCookie = true, response = response)
        val result = service.getPost(command)

        assertEquals(1L, result.id)
        verify(postViewCountUpdater, never()).increment(1L)
    }

    @Test
    fun `draft 글은 작성자 본인 admin 이면 조회 가능하다`() {
        val postRepository = mock(PostRepository::class.java)
        val response = mock(HttpServletResponse::class.java)
        val service = fixture(postRepository)
        val post = post(id = 2L, userId = 10L, status = PostStatus.DRAFT)
        `when`(postRepository.findById(2L)).thenReturn(Optional.of(post))

        val command = PostViewCommand(
            postId = 2L,
            hasViewedCookie = false,
            response = response,
            viewerUserId = 10L,
            viewerRole = UserRole.ADMIN,
        )
        val result = service.getPost(command)

        assertEquals(PostStatus.DRAFT, result.status)
    }

    @Test
    fun `draft 글은 비공개로 유지된다`() {
        val postRepository = mock(PostRepository::class.java)
        val response = mock(HttpServletResponse::class.java)
        val service = fixture(postRepository)
        val post = post(id = 3L, userId = 10L, status = PostStatus.DRAFT)
        `when`(postRepository.findById(3L)).thenReturn(Optional.of(post))

        val command = PostViewCommand(
            postId = 3L,
            hasViewedCookie = false,
            response = response,
            viewerUserId = 99L,
            viewerRole = UserRole.ADMIN,
        )
        val exception = assertThrows(CoreException::class.java) {
            service.getPost(command)
        }

        assertEquals(ErrorType.POST_NOT_FOUND, exception.errorType)
    }

    private fun fixture(
        postRepository: PostRepository = mock(PostRepository::class.java),
        postViewCountUpdater: PostViewCountUpdater = mock(PostViewCountUpdater::class.java),
    ): PostService {
        return PostService(
            postRepository = postRepository,
            postMarkdownConverter = mock(PostMarkdownConverter::class.java),
            cacheManager = mock(CacheManager::class.java),
            postViewCountUpdater = postViewCountUpdater,
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
