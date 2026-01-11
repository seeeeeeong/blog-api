package com.blog.api.core.domain

data class UserToken(
    val accessToken: String,
    val refreshToken: String,
    val refreshTokenId: String,
    val user: User
)
