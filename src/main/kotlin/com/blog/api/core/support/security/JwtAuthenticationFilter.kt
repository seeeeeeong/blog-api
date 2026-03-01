package com.blog.api.core.support.security

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.web.HttpServletRequestUtils
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

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return skipPaths.any { path.contains(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = HttpServletRequestUtils.extractBearerToken(request)
        if (token != null && jwtProvider.validateToken(token)) {
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
            // role 클레임 없는 토큰(OAuth 댓글 토큰 등) → SecurityContext 미설정, 요청 계속 진행
        }
    }

    companion object {
        private val skipPaths = listOf("/comments", "/auth/github")
    }
}
