package com.blog.api.core.api.config

import com.blog.api.core.support.properties.CorsProperties
import com.blog.api.core.support.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.handler.HandlerMappingIntrospector

private typealias AuthRegistry =
    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsProperties: CorsProperties,
) {
    companion object {
        private const val CORS_MAX_AGE_SECONDS = 3600L
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        introspector: HandlerMappingIntrospector,
    ): SecurityFilterChain {
        val mvc = MvcRequestMatcher.Builder(introspector)
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.authorizeHttpRequests { authorize ->
                authorizeAuth(authorize)
                authorizePosts(authorize, mvc)
                authorizeComments(authorize)
                authorizeImages(authorize)
                authorize.anyRequest().authenticated()
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    private fun authorizeAuth(authorize: AuthRegistry) {
        authorize
            .requestMatchers(HttpMethod.OPTIONS, "/**")
            .permitAll()
            .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus")
            .permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/users/login", "/api/v1/users/refresh")
            .permitAll()
    }

    private fun authorizePosts(
        authorize: AuthRegistry,
        mvc: MvcRequestMatcher.Builder,
    ) {
        authorize
            .requestMatchers(HttpMethod.POST, "/api/v1/posts")
            .hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/restore")
            .hasRole("ADMIN")
            .requestMatchers(
                mvc.pattern(HttpMethod.PUT, "/api/v1/posts/{postId:[0-9]+}"),
            ).hasRole("ADMIN")
            .requestMatchers(
                mvc.pattern(HttpMethod.DELETE, "/api/v1/posts/{postId:[0-9]+}"),
            ).hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/v1/posts/drafts")
            .hasRole("ADMIN")
            .requestMatchers(
                mvc.pattern(HttpMethod.GET, "/api/v1/posts/{postId:[0-9]+}/admin"),
            ).hasRole("ADMIN")
            .requestMatchers(
                HttpMethod.GET,
                "/api/v1/posts",
                "/api/v1/posts/popular",
                "/api/v1/posts/search",
                "/api/v1/posts/categories/**",
            ).permitAll()
            .requestMatchers(
                mvc.pattern(HttpMethod.GET, "/api/v1/posts/{postId:[0-9]+}"),
            ).permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/categories")
            .permitAll()
    }

    private fun authorizeComments(authorize: AuthRegistry) {
        authorize
            .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/*/comments/*/admin")
            .hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/v1/posts/*/comments/**")
            .permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/comments/**")
            .permitAll()
    }

    private fun authorizeImages(authorize: AuthRegistry) {
        authorize.requestMatchers("/api/images/**").hasRole("ADMIN")
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config =
            CorsConfiguration().apply {
                allowedOrigins =
                    corsProperties.allowedOrigins
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                allowedHeaders = listOf("Content-Type", "Authorization", "X-Requested-With")
                allowCredentials = true
                maxAge = CORS_MAX_AGE_SECONDS
            }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
