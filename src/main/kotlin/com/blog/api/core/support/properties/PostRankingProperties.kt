package com.blog.api.core.support.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "post-ranking")
data class PostRankingProperties(
    val localFallbackTtlSeconds: Long = 60,
    val localFallbackMaxSize: Long = 50,
)
