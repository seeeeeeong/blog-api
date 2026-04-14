package com.blog.api.core.api.config

import com.blog.api.core.support.properties.ImagePresignedProperties
import com.blog.api.core.support.properties.S3Properties
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    S3Properties::class,
    ImagePresignedProperties::class,
)
class AppConfig {

    @Bean
    fun kotlinModule(): KotlinModule {
        return KotlinModule.Builder()
            .enable(KotlinFeature.NullIsSameAsDefault)
            .build()
    }
}
