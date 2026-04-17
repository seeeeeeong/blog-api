package com.blog.api.core.domain.post

data class PostDeletedEvent(
    val postId: Long,
    val userId: Long,
    val thumbnailUrl: String?,
    val content: String,
)
