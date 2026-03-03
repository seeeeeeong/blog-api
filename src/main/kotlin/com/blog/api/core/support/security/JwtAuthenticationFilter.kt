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
        return path.startsWith(AUTH_GITHUB_PATH_PREFIX) || COMMENT_PATH_REGEX.matches(path)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
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
            // 타입/클레임 불일치 토큰은 SecurityContext를 비우고 요청 흐름을 유지한다.
        }
    }

    companion object {
        private const val AUTH_GITHUB_PATH_PREFIX = "/api/auth/github"
        private val COMMENT_PATH_REGEX = Regex("^/api/v1/posts/\\d+/comments(?:/.*)?$")
    }
}
