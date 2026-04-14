package com.blog.api.core.domain

data class CommentCreate(
    val postId: Long,
    val nickname: String,
    val password: String,
    val parentId: Long,
    val content: String,
)
