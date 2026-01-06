package com.blog.api.core.domain

import com.blog.api.enums.PostStatus
import java.time.LocalDateTime

data class Post(
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
)
