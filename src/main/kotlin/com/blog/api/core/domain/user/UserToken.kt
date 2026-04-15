package com.blog.api.core.domain.user

data class UserToken(
    val accessToken: String,
    val refreshToken: String,
)
