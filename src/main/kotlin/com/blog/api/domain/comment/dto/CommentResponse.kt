package com.blog.api.domain.comment.dto

import com.blog.api.domain.comment.entity.Comment
import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val postId: Long,
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String?,
    val parentId: Long?,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val replies: List<CommentResponse> = emptyList()
) {
    companion object {
        fun from(comment: Comment): CommentResponse {
            return CommentResponse(
                id = comment.id!!,
                postId = comment.postId,
                githubId = comment.githubId,
                githubUsername = comment.githubUsername,
                githubAvatarUrl = comment.githubAvatarUrl,
                parentId = comment.parentId,
                content = comment.content,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt
            )
        }

        fun fromWithReplies(comment: Comment, replies: List<Comment>): CommentResponse {
            return CommentResponse(
                id = comment.id!!,
                postId = comment.postId,
                githubId = comment.githubId,
                githubUsername = comment.githubUsername,
                githubAvatarUrl = comment.githubAvatarUrl,
                parentId = comment.parentId,
                content = comment.content,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
                replies = replies.map { from(it) }
            )
        }
    }
}