package com.blog.api.core.domain

data class CommentCreate(
    val postId: Long,
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String,
    val parentId: Long,
    val content: String
)
