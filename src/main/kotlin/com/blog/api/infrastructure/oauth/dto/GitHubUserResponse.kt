package com.blog.api.infrastructure.oauth.dto

data class GitHubUserResponse(
    val id: Long,
    val login: String,
    val avatarUrl: String?,
    val name: String?,
    val email: String?
)

data class GitHubTokenResponse(
    val accessToken: String,
    val tokenType: String,
    val scope: String
)

data class CommentAuthResponse(
    val commentToken: String,  
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String?
)