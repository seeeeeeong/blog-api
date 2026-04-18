package com.blog.api.core.api.config

import com.blog.api.core.support.properties.BlogAiProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class BlogAiClientConfig {
    @Bean
    fun blogAiRestClient(properties: BlogAiProperties): RestClient {
        val factory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs))
                setReadTimeout(Duration.ofMillis(properties.readTimeoutMs))
            }
        return RestClient
            .builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(factory)
            .build()
    }
}
