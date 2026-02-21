package com.blog.api.core.support.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "login-rate-limit")
data class LoginRateLimitProperties(
    val windowSeconds: Long = 600,
    val maxFailuresPerIp: Long = 30,
    val maxFailuresPerEmail: Long = 10,
    val maxFailuresPerIpEmail: Long = 5,
)
