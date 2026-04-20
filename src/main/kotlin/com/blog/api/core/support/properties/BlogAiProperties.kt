package com.blog.api.core.support.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "blog-ai")
data class BlogAiProperties(
    val baseUrl: String,
    val internalKey: String,
    val connectTimeoutMs: Long = 2_000,
    val readTimeoutMs: Long = 5_000,
    val backfillReadTimeoutMs: Long = 300_000,
    val webBaseUrl: String,
)
