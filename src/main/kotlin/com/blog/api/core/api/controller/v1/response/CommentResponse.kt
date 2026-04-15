package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.comment.CommentWithReplies
import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val postId: Long,
    val nickname: String,
    val parentId: Long?,
    val content: String,
    val contentHtml: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val replies: List<CommentResponse> = emptyList(),
) {
    companion object {
        fun of(commentWithReplies: CommentWithReplies): CommentResponse {
            return CommentResponse(
                id = commentWithReplies.comment.id,
                postId = commentWithReplies.comment.postId,
                nickname = commentWithReplies.comment.nickname,
                parentId = commentWithReplies.comment.parentId,
                content = commentWithReplies.comment.content,
                contentHtml = commentWithReplies.comment.contentHtml,
                createdAt = commentWithReplies.comment.createdAt,
                updatedAt = commentWithReplies.comment.updatedAt,
                replies = commentWithReplies.replies.map { reply ->
                    of(CommentWithReplies(reply, emptyList()))
                },
            )
        }
    }
}
