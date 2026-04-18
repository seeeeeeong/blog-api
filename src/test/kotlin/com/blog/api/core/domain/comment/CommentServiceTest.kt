package com.blog.api.core.domain.comment

import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.nickname.RandomNicknameGenerator
import com.blog.api.storage.comment.CommentEntity
import com.blog.api.storage.comment.CommentRepository
import com.blog.api.storage.post.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class CommentServiceTest {
    @Test
    fun `댓글 조회 시 contentHtml이 있으면 그대로 반환한다`() {
        val commentRepository = mock(CommentRepository::class.java)
        val service = fixture(commentRepository = commentRepository)

        val comment =
            CommentEntity(
                id = 1L,
                postId = 10L,
                nickname = "행복한 고양이",
                content = "hello",
                contentHtml = "<p>hello</p>",
            )
        `when`(commentRepository.findByPostId(10L)).thenReturn(listOf(comment))

        val result = service.getCommentsByPost(10L)

        assertEquals(1, result.size)
        assertEquals("<p>hello</p>", result[0].contentHtml)
    }

    @Test
    fun `댓글 조회 시 contentHtml이 null이면 content를 그대로 반환한다`() {
        val commentRepository = mock(CommentRepository::class.java)
        val service = fixture(commentRepository = commentRepository)

        val comment =
            CommentEntity(
                id = 1L,
                postId = 10L,
                nickname = "용감한 토끼",
                content = "plain text",
                contentHtml = null,
            )
        `when`(commentRepository.findByPostId(10L)).thenReturn(listOf(comment))

        val result = service.getCommentsByPost(10L)

        assertEquals(1, result.size)
        assertEquals("plain text", result[0].contentHtml)
    }

    private fun fixture(
        commentRepository: CommentRepository = mock(CommentRepository::class.java),
        postRepository: PostRepository = mock(PostRepository::class.java),
        postMarkdownConverter: PostMarkdownConverter = mock(PostMarkdownConverter::class.java),
        nicknameGenerator: RandomNicknameGenerator = RandomNicknameGenerator(),
    ): CommentService =
        CommentService(
            commentRepository = commentRepository,
            postRepository = postRepository,
            postMarkdownConverter = postMarkdownConverter,
            nicknameGenerator = nicknameGenerator,
        )
}
