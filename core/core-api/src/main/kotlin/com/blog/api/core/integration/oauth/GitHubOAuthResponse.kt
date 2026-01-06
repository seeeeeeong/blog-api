package com.blog.api.core.integration.oauth

import com.fasterxml.jackson.annotation.JsonProperty

data class GitHubOAuthTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String = "bearer",
    val scope: String? = null
)

data class GitHubOAuthUserResponse(
    val id: Long,
    val login: String,
    val avatarUrl: String?,
    val name: String?,
    val email: String?
)
