package com.blog.api.core.domain

data class CommentCreate(
    val postId: Long,
    val oauthId: String,
    val oauthUsername: String,
    val oauthAvatarUrl: String,
    val parentId: Long,
    val content: String
)
