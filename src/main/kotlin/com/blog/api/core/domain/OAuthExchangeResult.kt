package com.blog.api.core.domain

data class OAuthExchangeResult(
    val token: String,
    val user: OAuthUser
)
