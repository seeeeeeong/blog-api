package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.comment.Comment
import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val nickname: String,
    val content: String,
    val contentHtml: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(comment: Comment): CommentResponse {
            return CommentResponse(
                id = comment.id,
                nickname = comment.nickname,
                content = comment.content,
                contentHtml = comment.contentHtml,
                createdAt = comment.createdAt,
            )
        }
    }
}
