package com.blog.api.core.support.security

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.web.HttpServletRequestUtils
import io.github.oshai.kotlinlogging.KotlinLogging
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
) : OncePerRequestFilter() {
    companion object {
        private val log = KotlinLogging.logger {}
        private val COMMENT_PATH_REGEX = Regex("^/api/v1/posts/\\d+/comments(?:/.*)?$")
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return COMMENT_PATH_REGEX.matches(path)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = HttpServletRequestUtils.extractBearerToken(request)
        if (token != null && jwtProvider.validateUserAccessToken(token)) {
            authenticateUser(token)
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticateUser(token: String) {
        try {
            val userId = jwtProvider.getUserIdFromToken(token)
            val role = jwtProvider.getRoleFromToken(token)
            val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
            val authentication = UsernamePasswordAuthenticationToken(userId, null, authorities)
            SecurityContextHolder.getContext().authentication = authentication
        } catch (e: CoreException) {
            // Token with mismatched type or claims — clear SecurityContext and let the request continue.
            log.debug(e) { "Token authentication skipped: ${e.message}" }
        }
    }
}
