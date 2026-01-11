package com.blog.api.core.domain

data class OAuthUser(
    val id: Long,
    val login: String,
    val avatarUrl: String,
    val name: String,
    val email: String
)
