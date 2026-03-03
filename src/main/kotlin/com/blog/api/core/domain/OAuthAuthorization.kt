package com.blog.api.core.domain

data class OAuthAuthorization(
    val url: String,
    val state: String,
    val stateTtlSeconds: Long,
)
