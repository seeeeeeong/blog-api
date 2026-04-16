package com.blog.api.core.domain.post

import com.blog.api.core.enum.PostStatus
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
    fun `published 글을 조회할 수 있다`() {
        val postRepository = mock(PostRepository::class.java)
        val service = fixture(postRepository)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val result = service.getPost(1L)

        assertEquals(1L, result.id)
    }

    @Test
    fun `draft 글은 작성자 본인 admin 이면 조회 가능하다`() {
        val postRepository = mock(PostRepository::class.java)
        val service = fixture(postRepository)
        val post = post(id = 2L, userId = 10L, status = PostStatus.DRAFT)
        `when`(postRepository.findById(2L)).thenReturn(Optional.of(post))

        val result = service.getPost(2L, userId = 10L, isAdmin = true)

        assertEquals(PostStatus.DRAFT, result.status)
    }

    @Test
    fun `draft 글은 비공개로 유지된다`() {
        val postRepository = mock(PostRepository::class.java)
        val service = fixture(postRepository)
        val post = post(id = 3L, userId = 10L, status = PostStatus.DRAFT)
        `when`(postRepository.findById(3L)).thenReturn(Optional.of(post))

        val exception = assertThrows(CoreException::class.java) {
            service.getPost(3L, userId = 99L, isAdmin = true)
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

    @Test
    fun `renderHtml 마크다운 변환 실패 시 HTML 이스케이프된 content를 반환한다`() {
        val converter = mock(PostMarkdownConverter::class.java)
        val input = "<script>alert('xss')</script>"
        `when`(converter.convertToHtml(input)).thenThrow(RuntimeException("parse error"))
        val service = fixture(postMarkdownConverter = converter)

        val result = service.renderHtml(1L, input)

        assertEquals("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;", result)
    }

    @Test
    fun `renderHtml 변환 실패 시 캐시에 저장하지 않는다`() {
        val converter = mock(PostMarkdownConverter::class.java)
        val mockCache = mock(org.springframework.cache.Cache::class.java)
        val cacheManager = mock(CacheManager::class.java)
        `when`(cacheManager.getCache("post-html")).thenReturn(mockCache)
        `when`(mockCache.get(1L, String::class.java)).thenReturn(null)
        `when`(converter.convertToHtml("bad")).thenThrow(RuntimeException("parse error"))
        val service = PostService(
            postRepository = mock(PostRepository::class.java),
            postMarkdownConverter = converter,
            cacheManager = cacheManager,
            eventPublisher = mock(ApplicationEventPublisher::class.java),
        )

        service.renderHtml(1L, "bad")

        verify(mockCache, never()).put(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    fun `renderHtml 캐시가 없으면 변환 결과를 반환한다`() {
        val converter = mock(PostMarkdownConverter::class.java)
        `when`(converter.convertToHtml("# hello")).thenReturn("<h1>hello</h1>")
        val service = fixture(postMarkdownConverter = converter)

        val result = service.renderHtml(1L, "# hello")

        assertEquals("<h1>hello</h1>", result)
    }

    private fun fixture(
        postRepository: PostRepository = mock(PostRepository::class.java),
        eventPublisher: ApplicationEventPublisher = mock(ApplicationEventPublisher::class.java),
        postMarkdownConverter: PostMarkdownConverter = mock(PostMarkdownConverter::class.java),
    ): PostService {
        return PostService(
            postRepository = postRepository,
            postMarkdownConverter = postMarkdownConverter,
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
