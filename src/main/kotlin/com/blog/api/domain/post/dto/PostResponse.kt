package com.blog.api.domain.post.dto

import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.entity.PostStatus
import org.springframework.data.domain.Page
import java.time.LocalDateTime

data class PostResponse(
    val id: Long,
    val userId: Long,
    val categoryId: Long,
    val title: String,
    val content: String,
    val thumbnailUrl: String?,
    val viewCount: Int,
    val status: PostStatus,
    val tagIds: List<Long> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(post: Post, tagIds: List<Long> = emptyList()): PostResponse {
            return PostResponse(
                id = post.id!!,
                userId = post.userId,
                categoryId = post.categoryId,
                title = post.title,
                content = post.content,
                thumbnailUrl = post.thumbnailUrl,
                viewCount = post.viewCount,
                status = post.status,
                tagIds = tagIds,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }
    }
}

data class PostListResponse(
    val posts: List<PostResponse>,
    val totalPages: Int,
    val totalElements: Long,
    val currentPage: Int,
    val pageSize: Int
) {
    companion object {
        fun from(page: Page<Post>): PostListResponse {
            return PostListResponse(
                posts = page.content.map { PostResponse.from(it) },
                totalPages = page.totalPages,
                totalElements = page.totalElements,
                currentPage = page.number,
                pageSize = page.size
            )
        }

        fun from(page: Page<Post>, postTagsMap: Map<Long, List<Long>>): PostListResponse {
            return PostListResponse(
                posts = page.content.map { post ->
                    PostResponse.from(post, postTagsMap[post.id] ?: emptyList())
                },
                totalPages = page.totalPages,
                totalElements = page.totalElements,
                currentPage = page.number,
                pageSize = page.size
            )
        }
    }
}