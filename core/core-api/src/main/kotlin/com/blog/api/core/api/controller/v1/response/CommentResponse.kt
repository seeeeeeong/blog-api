package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.Comment
import com.blog.api.core.domain.CommentWithReplies
import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val postId: Long,
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String,
    val parentId: Long,
    val content: String,
    val contentHtml: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val replies: List<CommentResponse> = emptyList()
) {
    companion object {
        fun of(comment: Comment): CommentResponse {
            return CommentResponse(
                id = comment.id,
                postId = comment.postId,
                githubId = comment.githubId,
                githubUsername = comment.githubUsername,
                githubAvatarUrl = comment.githubAvatarUrl,
                parentId = comment.parentId,
                content = comment.content,
                contentHtml = comment.contentHtml,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt
            )
        }

        fun of(comments: List<CommentWithReplies>): List<CommentResponse> {
            return comments.map { of(it) }
        }

        fun of(commentWithReplies: CommentWithReplies): CommentResponse {
            return CommentResponse(
                id = commentWithReplies.comment.id,
                postId = commentWithReplies.comment.postId,
                githubId = commentWithReplies.comment.githubId,
                githubUsername = commentWithReplies.comment.githubUsername,
                githubAvatarUrl = commentWithReplies.comment.githubAvatarUrl,
                parentId = commentWithReplies.comment.parentId,
                content = commentWithReplies.comment.content,
                contentHtml = commentWithReplies.comment.contentHtml,
                createdAt = commentWithReplies.comment.createdAt,
                updatedAt = commentWithReplies.comment.updatedAt,
                replies = commentWithReplies.replies.map { of(it) }
            )
        }
    }
}
