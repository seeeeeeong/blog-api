package com.blog.api.storage

import com.blog.api.core.domain.Post

fun PostEntity.toPost(): Post = Post(
    id = id!!,
    userId = userId,
    categoryId = categoryId,
    title = title,
    content = content,
    thumbnailUrl = thumbnailUrl,
    viewCount = viewCount,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
