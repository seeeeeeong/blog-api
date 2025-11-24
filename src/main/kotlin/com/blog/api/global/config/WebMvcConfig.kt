package com.blog.api.global.config

import com.blog.api.global.web.resolver.AuthUserArgumentResolver
import com.blog.api.global.web.resolver.ClientIpArgumentResolver
import com.blog.api.global.web.resolver.GitHubAuthArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authUserArgumentResolver: AuthUserArgumentResolver,
    private val clientIpArgumentResolver: ClientIpArgumentResolver,
    private val gitHubAuthArgumentResolver: GitHubAuthArgumentResolver
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authUserArgumentResolver)
        resolvers.add(clientIpArgumentResolver)
        resolvers.add(gitHubAuthArgumentResolver)
    }
}