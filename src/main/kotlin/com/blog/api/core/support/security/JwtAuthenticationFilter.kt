package com.blog.api.core.support.security

import com.blog.api.core.domain.UserTokenExtractor
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userTokenExtractor: UserTokenExtractor
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return skipPaths.any { path.contains(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        extractToken(request)?.takeIf { jwtProvider.validateToken(it) }
            ?.let { authenticateUser(it) }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? =
        userTokenExtractor.extract(request.getHeader(AUTHORIZATION_HEADER))

    private fun authenticateUser(token: String) {
        val userId = jwtProvider.getUserIdFromToken(token)
        val role = jwtProvider.getRoleFromToken(token)
        val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
        val authentication = UsernamePasswordAuthenticationToken(userId, null, authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private val skipPaths = listOf("/comments", "/auth/github")
    }
}
