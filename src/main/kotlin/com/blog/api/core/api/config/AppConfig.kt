package com.blog.api.core.api.config

import com.blog.api.core.support.properties.CloudinaryProperties
import com.blog.api.core.support.properties.EmbeddingProperties
import com.blog.api.core.support.properties.OAuthProperties
import com.blog.api.core.support.properties.RefreshTokenProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
@EnableConfigurationProperties(
    OAuthProperties::class,
    CloudinaryProperties::class,
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
