package com.blog.api.core.api.config

import com.blog.api.core.support.security.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
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
@EnableMethodSecurity(prePostEnabled = true)
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
                    // CORS preflight
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // Health check
                    .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()

                    // Swagger UI
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                    // Auth APIs
                    .requestMatchers("/api/auth/github/**").permitAll()
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

                    // Public Read APIs
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/posts",
                        "/api/v1/posts/popular",
                        "/api/v1/posts/search",
                        "/api/v1/posts/categories/**"
                    ).permitAll()
                    .requestMatchers(
                        mvc.pattern(HttpMethod.GET, "/api/v1/posts/{postId:[0-9]+}")
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()

                    // Comment read APIs (public)
                    .requestMatchers(HttpMethod.GET, "/api/v1/posts/*/comments/**").permitAll()
                    // Comment write APIs (auth delegated to @OAuthUser ArgumentResolver)
                    .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/comments/**").permitAll()
                    .requestMatchers(HttpMethod.PUT, "/api/v1/posts/*/comments/**").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/*/comments/**").permitAll()

                    // Image APIs (Admin only)
                    .requestMatchers("/api/images/**").hasRole("ADMIN")

                    // All other requests need authentication
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
            exposedHeaders = listOf("Authorization", "GitHub-Username", "GitHub-Avatar-Url")
            allowCredentials = true
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
