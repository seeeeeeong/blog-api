package com.blog.api.core.api.config

import com.blog.api.core.support.properties.OAuthUserProperties
import com.blog.api.core.support.properties.ImagePresignedProperties
import com.blog.api.core.support.properties.LoginRateLimitProperties
import com.blog.api.core.support.properties.S3Properties
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
@EnableConfigurationProperties(
    OAuthUserProperties::class,
    S3Properties::class,
    ImagePresignedProperties::class,
    LoginRateLimitProperties::class,
)
class AppConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(5))
            .build()
    }

    @Bean
    fun kotlinModule(): KotlinModule {
        return KotlinModule.Builder()
            .enable(KotlinFeature.NullIsSameAsDefault)
            .build()
    }
}
