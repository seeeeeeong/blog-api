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
    val contentHtml: String,
    val thumbnailUrl: String?,
    val viewCount: Int,
    val status: PostStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(post: Post, contentHtml: String): PostResponse {
            return PostResponse(
                id = post.id!!,
                userId = post.userId,
                categoryId = post.categoryId,
                title = post.title,
                content = post.content,
                contentHtml = contentHtml,
                thumbnailUrl = post.thumbnailUrl,
                viewCount = post.viewCount,
                status = post.status,
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
        fun from(page: Page<Post>, convertToHtml: (String) -> String): PostListResponse {
            return PostListResponse(
                posts = page.content.map { PostResponse.from(it, convertToHtml(it.content)) },
                totalPages = page.totalPages,
                totalElements = page.totalElements,
                currentPage = page.number,
                pageSize = page.size
            )
        }
    }
}