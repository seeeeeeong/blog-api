package com.blog.api.core.domain

data class OAuthLogin(
    val token: String,
    val user: OAuthUser
)
