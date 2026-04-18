package com.blog.api.core.domain.postoutbox

import com.blog.api.core.support.properties.BlogAiProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class BlogAiSyncClient(
    private val blogAiRestClient: RestClient,
    private val blogAiProperties: BlogAiProperties,
) {
    fun send(rawPayload: String) {
        blogAiRestClient
            .post()
            .uri("/api/v1/internal/blog-posts/sync")
            .header("X-Internal-Key", blogAiProperties.internalKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(rawPayload)
            .retrieve()
            .toBodilessEntity()
    }
}
