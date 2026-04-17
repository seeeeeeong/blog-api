package com.blog.api.core.domain.comment

data class CommentCreate(
    val postId: Long,
    val content: String,
)
