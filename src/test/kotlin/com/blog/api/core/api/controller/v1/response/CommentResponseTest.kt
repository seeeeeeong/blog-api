package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.comment.Comment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class CommentResponseTest {
    @Test
    fun `createdAt is exposed with UTC offset`() {
        val comment =
            Comment(
                id = 1L,
                postId = 10L,
                nickname = "guest",
                content = "hello",
                contentHtml = "hello",
                createdAt = LocalDateTime.of(2026, 4, 26, 1, 0),
            )

        val response = CommentResponse.of(comment)

        assertEquals(OffsetDateTime.of(2026, 4, 26, 1, 0, 0, 0, ZoneOffset.UTC), response.createdAt)
    }
}
