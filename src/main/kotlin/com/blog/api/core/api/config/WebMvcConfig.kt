package com.blog.api.core.api.config

import com.blog.api.core.support.auth.AuthUserArgumentResolver
import com.blog.api.core.support.web.ClientFingerprintArgumentResolver
import com.blog.api.core.support.auth.OAuthAuthArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authUserArgumentResolver: AuthUserArgumentResolver,
    private val clientFingerprintArgumentResolver: ClientFingerprintArgumentResolver,
    private val oauthAuthArgumentResolver: OAuthAuthArgumentResolver
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authUserArgumentResolver)
        resolvers.add(clientFingerprintArgumentResolver)
        resolvers.add(oauthAuthArgumentResolver)
    }
}
