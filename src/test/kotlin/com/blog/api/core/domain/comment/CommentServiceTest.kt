package com.blog.api.core.domain.comment

import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.storage.comment.CommentEntity
import com.blog.api.storage.comment.CommentRepository
import com.blog.api.storage.post.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder

class CommentServiceTest {

    @Test
    fun `댓글 조회 시 contentHtml이 null이고 변환 실패하면 HTML 이스케이프된 content를 반환한다`() {
        val commentRepository = mock(CommentRepository::class.java)
        val converter = mock(PostMarkdownConverter::class.java)
        val service = fixture(commentRepository = commentRepository, postMarkdownConverter = converter)

        val input = "<script>alert('xss')</script>"
        val comment = CommentEntity(
            id = 1L,
            postId = 10L,
            nickname = "tester",
            password = "encoded",
            parentId = null,
            content = input,
            contentHtml = null,
        )
        `when`(commentRepository.findRootCommentsByPostId(10L)).thenReturn(listOf(comment))
        `when`(commentRepository.findReplyCommentsByPostId(10L)).thenReturn(emptyList())
        `when`(converter.convertToHtml(input)).thenThrow(RuntimeException("parse error"))

        val result = service.getCommentsByPost(10L)

        assertEquals(1, result.size)
        assertEquals("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;", result[0].comment.contentHtml)
    }

    @Test
    fun `댓글 조회 시 contentHtml이 있으면 변환하지 않고 그대로 반환한다`() {
        val commentRepository = mock(CommentRepository::class.java)
        val converter = mock(PostMarkdownConverter::class.java)
        val service = fixture(commentRepository = commentRepository, postMarkdownConverter = converter)

        val comment = CommentEntity(
            id = 1L,
            postId = 10L,
            nickname = "tester",
            password = "encoded",
            parentId = null,
            content = "hello",
            contentHtml = "<p>hello</p>",
        )
        `when`(commentRepository.findRootCommentsByPostId(10L)).thenReturn(listOf(comment))
        `when`(commentRepository.findReplyCommentsByPostId(10L)).thenReturn(emptyList())

        val result = service.getCommentsByPost(10L)

        assertEquals(1, result.size)
        assertEquals("<p>hello</p>", result[0].comment.contentHtml)
    }

    private fun fixture(
        commentRepository: CommentRepository = mock(CommentRepository::class.java),
        postRepository: PostRepository = mock(PostRepository::class.java),
        postMarkdownConverter: PostMarkdownConverter = mock(PostMarkdownConverter::class.java),
        passwordEncoder: PasswordEncoder = mock(PasswordEncoder::class.java),
    ): CommentService {
        return CommentService(
            commentRepository = commentRepository,
            postRepository = postRepository,
            postMarkdownConverter = postMarkdownConverter,
            passwordEncoder = passwordEncoder,
        )
    }
}
