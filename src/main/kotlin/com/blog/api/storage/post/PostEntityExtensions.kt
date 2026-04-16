package com.blog.api.storage.post

import com.blog.api.core.domain.post.Post

fun PostEntity.toPost(): Post = Post(
    id = requireNotNull(id) { "PostEntity.id must not be null after persistence" },
    userId = userId,
    categoryId = categoryId,
    title = title,
    content = content,
    thumbnailUrl = thumbnailUrl,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
