package com.blog.api.core.api.config

import com.blog.api.core.support.security.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
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

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    @param:Value("\${cors.allowed-origins}") private val corsAllowedOrigins: String,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity, introspector: HandlerMappingIntrospector): SecurityFilterChain {
        val mvc = MvcRequestMatcher.Builder(introspector)
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()

                    // Auth APIs
                    .requestMatchers(HttpMethod.POST, "/api/v1/users/login", "/api/v1/users/refresh").permitAll()

                    // Post write APIs (ADMIN only)
                    .requestMatchers(HttpMethod.POST, "/api/v1/posts").hasRole("ADMIN")
                    .requestMatchers(
                        mvc.pattern(HttpMethod.PUT, "/api/v1/posts/{postId:[0-9]+}")
                    ).hasRole("ADMIN")
                    .requestMatchers(
                        mvc.pattern(HttpMethod.DELETE, "/api/v1/posts/{postId:[0-9]+}")
                    ).hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/posts/drafts").hasRole("ADMIN")
                    .requestMatchers(
                        mvc.pattern(HttpMethod.GET, "/api/v1/posts/{postId:[0-9]+}/admin")
                    ).hasRole("ADMIN")

                    // Public Read APIs
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/posts",
                        "/api/v1/posts/popular",
                        "/api/v1/posts/search",
                        "/api/v1/posts/categories/**",
                    ).permitAll()
                    .requestMatchers(
                        mvc.pattern(HttpMethod.GET, "/api/v1/posts/{postId:[0-9]+}")
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()

                    // Admin comment delete (must come before general comment rules)
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/*/comments/*/admin").hasRole("ADMIN")

                    // Comment APIs (public)
                    .requestMatchers(HttpMethod.GET, "/api/v1/posts/*/comments/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/comments/**").permitAll()
                    .requestMatchers(HttpMethod.PUT, "/api/v1/posts/*/comments/**").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/*/comments/**").permitAll()

                    // Image APIs (Admin only)
                    .requestMatchers("/api/images/**").hasRole("ADMIN")

                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = corsAllowedOrigins.split(",").map { it.trim() }.filter { it.isNotBlank() }
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Content-Type", "Authorization", "X-Requested-With")
            allowCredentials = true
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
