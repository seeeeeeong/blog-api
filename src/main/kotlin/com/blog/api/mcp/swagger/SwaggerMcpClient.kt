package com.blog.api.mcp.swagger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.OpenAPI
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class SwaggerMcpClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val openApi: OpenAPI,
    private val properties: SwaggerMcpProperties
) {

    fun fetchOpenApiJson(): JsonNode {
        val baseUrl = resolveBaseUrl().trimEnd('/')
        val docsUrl = baseUrl + properties.apiDocsPath
        val response = restTemplate.getForObject(docsUrl, String::class.java).orEmpty()
        return objectMapper.readTree(response)
    }

    private fun resolveBaseUrl(): String {
        val configured = properties.baseUrl.trim()
        if (configured.isNotEmpty()) {
            return configured
        }
        val serverUrl = openApi.servers?.firstOrNull()?.url
        if (!serverUrl.isNullOrBlank()) {
            return serverUrl
        }
        return "http://localhost:8080"
    }
}
