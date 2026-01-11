package com.blog.api.core.support.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "embedding")
data class EmbeddingProperties(
    val maxTokens: Int = 8191,
    val charsPerToken: Int = 4,
    val vectorDimension: Int = 1536,
    val searchMaxDistance: Double = 1.2
) {
    val maxContentLength: Int
        get() = maxTokens * charsPerToken
}
