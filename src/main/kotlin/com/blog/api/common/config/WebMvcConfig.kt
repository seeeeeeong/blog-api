package com.blog.api.common.config

import com.blog.api.common.web.interceptor.LoggingInterceptor
import com.blog.api.common.web.resolver.AuthUserArgumentResolver
import com.blog.api.common.web.resolver.ClientIpArgumentResolver
import com.blog.api.common.web.resolver.GitHubAuthArgumentResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authUserArgumentResolver: AuthUserArgumentResolver,
    private val clientIpArgumentResolver: ClientIpArgumentResolver,
    private val gitHubAuthArgumentResolver: GitHubAuthArgumentResolver,
    private val loggingInterceptor: LoggingInterceptor
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authUserArgumentResolver)
        resolvers.add(clientIpArgumentResolver)
        resolvers.add(gitHubAuthArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/api/**")
    }
}