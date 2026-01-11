package com.blog.api.core.support.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "github.oauth")
data class OAuthProperties(
    val clientId: String,
    val clientSecret: String,
    val tokenUrl: String = "https://github.com/login/oauth/access_token",
    val userApiUrl: String = "https://api.github.com/user"
)
