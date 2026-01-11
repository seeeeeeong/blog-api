package com.blog.api.core.domain

import com.blog.api.core.enum.PostStatus

data class PostCreate(
    val userId: Long,
    val categoryId: Long,
    val title: String,
    val content: String,
    val thumbnailUrl: String,
    val status: PostStatus
)
