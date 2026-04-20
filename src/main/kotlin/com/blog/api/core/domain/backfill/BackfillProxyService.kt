package com.blog.api.core.domain.backfill

import com.blog.api.core.support.properties.BlogAiProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class BackfillProxyService(
    private val blogAiRestClient: RestClient,
    private val blogAiProperties: BlogAiProperties,
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun backfillContent(batchSize: Int): Int {
        log.info { "Backfill content triggered: batchSize=$batchSize" }
        val result =
            blogAiRestClient
                .post()
                .uri("/api/v1/admin/backfill/content?batchSize=$batchSize")
                .header("X-Admin-Key", blogAiProperties.internalKey)
                .retrieve()
                .body(BackfillResponse::class.java)
        return result?.data ?: 0
    }

    fun backfillEmbedding(): Int {
        log.info { "Backfill embedding triggered" }
        val result =
            blogAiRestClient
                .post()
                .uri("/api/v1/admin/backfill/embedding")
                .header("X-Admin-Key", blogAiProperties.internalKey)
                .retrieve()
                .body(BackfillResponse::class.java)
        return result?.data ?: 0
    }

    fun getStats(): Map<String, Any> {
        val result =
            blogAiRestClient
                .get()
                .uri("/api/v1/admin/stats")
                .header("X-Admin-Key", blogAiProperties.internalKey)
                .retrieve()
                .body(StatsResponse::class.java)
        return result?.data ?: emptyMap()
    }
}

private data class BackfillResponse(
    val result: String,
    val data: Int?,
)

private data class StatsResponse(
    val result: String,
    val data: Map<String, Any>?,
)
