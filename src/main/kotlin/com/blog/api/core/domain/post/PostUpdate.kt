package com.blog.api.core.domain.post

import com.blog.api.core.enum.PostStatus

data class PostUpdate(
    val categoryId: Long,
    val title: String,
    val content: String,
    val thumbnailUrl: String?,
    val status: PostStatus
)
