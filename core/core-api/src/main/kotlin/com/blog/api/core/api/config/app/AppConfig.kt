package com.blog.api.core.api.config.app

import com.blog.api.core.api.config.properties.CloudinaryProperties
import com.blog.api.core.api.config.properties.EmbeddingProperties
import com.blog.api.core.api.config.properties.GitHubProperties
import com.blog.api.core.api.config.properties.RefreshTokenProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(
    GitHubProperties::class,
    CloudinaryProperties::class,
    EmbeddingProperties::class,
    RefreshTokenProperties::class
)
class AppConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}
