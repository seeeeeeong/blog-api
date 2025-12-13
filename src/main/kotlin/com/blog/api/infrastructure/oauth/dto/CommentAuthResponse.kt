package com.blog.api.infrastructure.oauth.dto

data class CommentAuthResponse(
    val commentToken: String,
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String?
)
