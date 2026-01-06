package com.blog.api.core.domain

import com.blog.api.enums.PostStatus

data class PostUpdate(
    val categoryId: Long,
    val title: String,
    val content: String,
    val thumbnailUrl: String,
    val status: PostStatus
)
