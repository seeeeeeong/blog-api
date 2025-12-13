package com.blog.api.common.config

import com.blog.api.common.security.JwtAuthenticationFilter
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    // Health check
                    .requestMatchers("/api/health").permitAll()

                    // Swagger UI
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                    // Auth APIs
                    .requestMatchers("/api/auth/github/callback").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users/refresh").permitAll()

                    // Public Read APIs
                    .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/search", "/api/posts/popular", "/api/posts/categories/**", "/api/posts/{postId:[0-9]+}").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/posts/{postId:[0-9]+}/comments", "/api/comments/recent").permitAll()

                    // All other requests need authentication
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
