package com.blog.api.mcp.swagger

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerMcpConfig {

    @Bean
    fun swaggerMcpToolCallbacks(swaggerMcpTools: SwaggerMcpTools): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder()
            .toolObjects(swaggerMcpTools)
            .build()
    }
}
