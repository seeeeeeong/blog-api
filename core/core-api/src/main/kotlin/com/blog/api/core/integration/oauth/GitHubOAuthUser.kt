package com.blog.api.core.integration.oauth

data class GitHubOAuthUser(
    val id: Long,
    val login: String,
    val avatarUrl: String?,
    val name: String?,
    val email: String?
)