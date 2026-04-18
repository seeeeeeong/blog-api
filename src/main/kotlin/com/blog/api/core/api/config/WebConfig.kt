package com.blog.api.core.api.config

import com.blog.api.core.support.auth.AdminArgumentResolver
import com.blog.api.core.support.auth.CurrentUserArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val adminArgumentResolver: AdminArgumentResolver,
    private val currentUserArgumentResolver: CurrentUserArgumentResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(adminArgumentResolver)
        resolvers.add(currentUserArgumentResolver)
    }
}
