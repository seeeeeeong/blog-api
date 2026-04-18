package com.blog.api.core.domain.post

import com.blog.api.core.enum.PostStatus
import java.time.LocalDateTime

data class Post(
    val id: Long,
    val userId: Long,
    val categoryId: Long,
    val title: String,
    val content: String,
    val thumbnailUrl: String?,
    val status: PostStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
