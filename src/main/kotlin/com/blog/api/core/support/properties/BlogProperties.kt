package com.blog.api.core.support.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "blog")
data class BlogProperties(
    val slowRequestThresholdMs: Long = 700,
)
