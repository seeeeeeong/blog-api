package com.blog.api.core.api.config

import com.blog.api.core.support.properties.CloudinaryProperties
import com.blog.api.core.support.properties.EmbeddingProperties
import com.blog.api.core.support.properties.OAuthProperties
import com.blog.api.core.support.properties.RefreshTokenProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(
    OAuthProperties::class,
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
