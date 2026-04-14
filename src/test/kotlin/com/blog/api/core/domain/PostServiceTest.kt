package com.blog.api.core.domain

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.PostEntity
import com.blog.api.storage.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.cache.CacheManager
import java.util.Optional

class PostServiceTest {

    @Test
    fun `published 글 조회 시 조회수를 증가시킨다`() {
        val postRepository = mock(PostRepository::class.java)
        val service = fixture(postRepository)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val result = service.getPost(1L, "127.0.0.1")

        assertEquals(1L, result.id)
        verify(postRepository).incrementViewCount(1L)
    }

    @Test
    fun `draft 글은 작성자 본인 admin 이면 조회 가능하다`() {
        val postRepository = mock(PostRepository::class.java)
        val service = fixture(postRepository)
        val post = post(id = 2L, userId = 10L, status = PostStatus.DRAFT)
        `when`(postRepository.findById(2L)).thenReturn(Optional.of(post))

        val result = service.getPost(2L, "127.0.0.1", userId = 10L, isAdmin = true)

        assertEquals(PostStatus.DRAFT, result.status)
    }

    @Test
    fun `draft 글은 비공개로 유지된다`() {
        val postRepository = mock(PostRepository::class.java)
        val service = fixture(postRepository)
        val post = post(id = 3L, userId = 10L, status = PostStatus.DRAFT)
        `when`(postRepository.findById(3L)).thenReturn(Optional.of(post))

        val exception = assertThrows(CoreException::class.java) {
            service.getPost(3L, "127.0.0.1", userId = 99L, isAdmin = true)
        }

        assertEquals(ErrorType.POST_NOT_FOUND, exception.errorType)
    }

    private fun fixture(
        postRepository: PostRepository = mock(PostRepository::class.java),
    ): PostService {
        return PostService(
            postRepository = postRepository,
            postMarkdownConverter = mock(PostMarkdownConverter::class.java),
            cacheManager = mock(CacheManager::class.java),
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
