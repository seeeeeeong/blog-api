package com.blog.api.core.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Value("\${spring.profiles.active:local}")
    private lateinit var profile: String

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "bearerAuth"

        val servers = if (profile == "prod") {
            listOf(
                Server().url("https://web-production-405ba.up.railway.app").description("Production Server")
            )
        } else {
            listOf(
                Server().url("http://localhost:8080").description("Local Server")
            )
        }

        return OpenAPI()
            .info(
                Info()
                    .title("Blog API")
                    .description("개인 블로그 REST API 문서")
                    .version("1.0.0")
            )
            .servers(servers)
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
    }
}
