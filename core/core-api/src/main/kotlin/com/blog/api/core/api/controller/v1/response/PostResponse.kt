package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.Post
import com.blog.api.enums.PostStatus
import java.time.LocalDateTime

data class PostResponse(
    val id: Long,
    val userId: Long,
    val categoryId: Long,
    val title: String,
    val content: String,
    val contentHtml: String,
    val thumbnailUrl: String,
    val viewCount: Int,
    val status: PostStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun of(post: Post): PostResponse {
            return PostResponse(
                id = post.id,
                userId = post.userId,
                categoryId = post.categoryId,
                title = post.title,
                content = post.content,
                contentHtml = post.contentHtml,
                thumbnailUrl = post.thumbnailUrl,
                viewCount = post.viewCount,
                status = post.status,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }

        fun of(posts: List<Post>): List<PostResponse> {
            return posts.map { of(it) }
        }
    }
}
