package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.post.Post
import com.blog.api.core.enum.PostStatus
import java.time.LocalDateTime

data class PostSummaryResponse(
    val id: Long,
    val userId: Long,
    val categoryId: Long,
    val title: String,
    val excerpt: String,
    val thumbnailUrl: String,
    val viewCount: Int,
    val status: PostStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        private const val EXCERPT_MAX_LENGTH = 140

        fun of(post: Post): PostSummaryResponse {
            return PostSummaryResponse(
                id = post.id,
                userId = post.userId,
                categoryId = post.categoryId,
                title = post.title,
                excerpt = post.content.take(EXCERPT_MAX_LENGTH),
                thumbnailUrl = post.thumbnailUrl.orEmpty(),
                viewCount = post.viewCount,
                status = post.status,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }
    }
}
