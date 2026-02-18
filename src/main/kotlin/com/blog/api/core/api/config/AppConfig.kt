package com.blog.api.core.api.config

import com.blog.api.core.support.properties.EmbeddingProperties
import com.blog.api.core.support.properties.OAuthProperties
import com.blog.api.core.support.properties.RefreshTokenProperties
import com.blog.api.core.support.properties.S3Properties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
@EnableConfigurationProperties(
    OAuthProperties::class,
    S3Properties::class,
    EmbeddingProperties::class,
    RefreshTokenProperties::class
)
class AppConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(5))
            .build()
    }
}
