package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.post.Post
import com.blog.api.core.enum.PostStatus
import java.time.LocalDateTime

data class PostResponse(
    val id: Long,
    val userId: Long,
    val categoryId: Long,
    val title: String,
    val content: String,
    val contentHtml: String,
    val thumbnailUrl: String?,
    val status: PostStatus,
    val comments: List<CommentResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun of(post: Post, contentHtml: String, comments: List<CommentResponse> = emptyList()): PostResponse {
            return PostResponse(
                id = post.id,
                userId = post.userId,
                categoryId = post.categoryId,
                title = post.title,
                content = post.content,
                contentHtml = contentHtml,
                thumbnailUrl = post.thumbnailUrl,
                status = post.status,
                comments = comments,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
            )
        }
    }
}
