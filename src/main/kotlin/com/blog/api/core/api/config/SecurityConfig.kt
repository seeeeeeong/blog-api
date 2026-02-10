package com.blog.api.core.api.config

import com.blog.api.core.support.security.JwtAuthenticationFilter
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
import org.springframework.web.servlet.handler.HandlerMappingIntrospector

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
    fun securityFilterChain(http: HttpSecurity, introspector: HandlerMappingIntrospector): SecurityFilterChain {
        val mvc = MvcRequestMatcher.Builder(introspector)
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
                    .requestMatchers("/api/auth/github/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/users/login", "/api/v1/users/refresh").permitAll()

                    // Public Read APIs
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/posts",
                        "/api/v1/posts/search/similarity",
                        "/api/v1/posts/popular",
                        "/api/v1/posts/categories/**"
                    ).permitAll()
                    .requestMatchers(
                        mvc.pattern(HttpMethod.GET, "/api/v1/posts/{postId:[0-9]+}")
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()

                    // Comment APIs (GitHub OAuth token used instead of admin JWT)
                    .requestMatchers("/api/v1/posts/*/comments/**").permitAll()

                    // Image APIs (Admin only)
                    .requestMatchers("/api/images/**").hasRole("ADMIN")

                    // All other requests need authentication
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
