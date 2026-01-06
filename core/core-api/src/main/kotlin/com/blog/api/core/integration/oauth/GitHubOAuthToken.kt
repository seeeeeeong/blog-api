package com.blog.api.core.integration.oauth

data class GitHubOAuthToken(
    val accessToken: String,
    val tokenType: String,
    val scope: String?
)


