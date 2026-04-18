package com.blog.api.core.domain.post

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.converter.MarkdownRenderer
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.post.PostEntity
import com.blog.api.storage.post.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.cache.CacheManager
import java.util.Optional

class PostReaderTest {
    @Test
    fun `published 글을 조회할 수 있다`() {
        val postRepository = mock(PostRepository::class.java)
        val reader = fixture(postRepository)
        val post = post(id = 1L, userId = 10L, status = PostStatus.PUBLISHED)
        `when`(postRepository.findById(1L)).thenReturn(Optional.of(post))

        val result = reader.getPost(1L)

        assertEquals(1L, result.id)
    }

    @Test
    fun `draft 글은 작성자 본인 admin 이면 조회 가능하다`() {
        val postRepository = mock(PostRepository::class.java)
        val reader = fixture(postRepository)
        val post = post(id = 2L, userId = 10L, status = PostStatus.DRAFT)
        `when`(postRepository.findById(2L)).thenReturn(Optional.of(post))

        val result = reader.getPost(2L, userId = 10L, isAdmin = true)

        assertEquals(PostStatus.DRAFT, result.status)
    }

    @Test
    fun `draft 글은 비공개로 유지된다`() {
        val postRepository = mock(PostRepository::class.java)
        val reader = fixture(postRepository)
        val post = post(id = 3L, userId = 10L, status = PostStatus.DRAFT)
        `when`(postRepository.findById(3L)).thenReturn(Optional.of(post))

        val exception =
            assertThrows(CoreException::class.java) {
                reader.getPost(3L, userId = 99L, isAdmin = true)
            }

        assertEquals(ErrorType.POST_NOT_FOUND, exception.errorType)
    }

    @Test
    fun `renderHtml 마크다운 변환 실패 시 HTML 이스케이프된 content를 반환한다`() {
        val markdownRenderer = mock(MarkdownRenderer::class.java)
        val input = "<script>alert('xss')</script>"
        val escaped = "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"
        `when`(markdownRenderer.renderOrEscape(input, 1L)).thenReturn(escaped)
        val reader = fixture(markdownRenderer = markdownRenderer)

        val result = reader.renderHtml(1L, input)

        assertEquals(escaped, result)
    }

    @Test
    fun `renderHtml 캐시가 없으면 변환 결과를 반환한다`() {
        val markdownRenderer = mock(MarkdownRenderer::class.java)
        `when`(markdownRenderer.renderOrEscape("# hello", 1L)).thenReturn("<h1>hello</h1>")
        val reader = fixture(markdownRenderer = markdownRenderer)

        val result = reader.renderHtml(1L, "# hello")

        assertEquals("<h1>hello</h1>", result)
    }

    private fun fixture(
        postRepository: PostRepository = mock(PostRepository::class.java),
        markdownRenderer: MarkdownRenderer = mock(MarkdownRenderer::class.java),
    ) = PostReader(
        postRepository = postRepository,
        markdownRenderer = markdownRenderer,
        cacheManager = mock(CacheManager::class.java),
    )

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
