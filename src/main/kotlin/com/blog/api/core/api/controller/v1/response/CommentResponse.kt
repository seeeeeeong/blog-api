package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.comment.Comment
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class CommentResponse(
    val id: Long,
    val nickname: String,
    val content: String,
    val contentHtml: String,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun of(comment: Comment): CommentResponse =
            CommentResponse(
                id = comment.id,
                nickname = comment.nickname,
                content = comment.content,
                contentHtml = comment.contentHtml,
                createdAt = comment.createdAt.atOffset(ZoneOffset.UTC),
            )
    }
}
