package com.blog.api.core.domain

data class Token(
    val accessToken: String,
    val refreshToken: String,
    val refreshTokenId: String,
    val user: User
)
