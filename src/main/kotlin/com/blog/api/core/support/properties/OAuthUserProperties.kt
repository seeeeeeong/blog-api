package com.blog.api.core.support.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.github")
data class OAuthUserProperties(
    val clientId: String,
    val clientSecret: String,
    val authorizationUri: String = "https://github.com/login/oauth/authorize",
    val tokenUrl: String = "https://github.com/login/oauth/access_token",
    val userApiUrl: String = "https://api.github.com/user",
    val scope: String = "read:user user:email",
    val callbackUrl: String = "",
    val redirectUrl: String = "http://localhost:5173/auth/callback",
)
