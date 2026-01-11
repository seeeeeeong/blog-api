package com.blog.api.core.api.config

import com.cloudinary.Cloudinary
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CloudinaryConfig {

    @Bean
    fun cloudinary(
        @Value("\${cloudinary.cloud-name}") cloudName: String,
        @Value("\${cloudinary.api-key}") apiKey: String,
        @Value("\${cloudinary.api-secret}") apiSecret: String
    ): Cloudinary {
        return Cloudinary(
            mapOf(
                "cloud_name" to cloudName,
                "api_key" to apiKey,
                "api_secret" to apiSecret,
                "secure" to true
            )
        )
    }
}