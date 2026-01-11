package com.blog.api.core.domain

data class OAuthToken(
    val accessToken: String,
    val tokenType: String,
    val scope: String
)
