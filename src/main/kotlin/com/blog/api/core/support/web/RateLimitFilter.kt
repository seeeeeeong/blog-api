package com.blog.api.core.support.web

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@Component
class RateLimitFilter : OncePerRequestFilter() {

    private val loginLimiter: LoadingCache<String, AtomicInteger> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build { AtomicInteger(0) }

    private val commentLimiter: LoadingCache<String, AtomicInteger> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build { AtomicInteger(0) }

    companion object {
        private const val LOGIN_MAX_PER_MINUTE = 10
        private const val COMMENT_MAX_PER_MINUTE = 5
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (request.method != "POST") return true
        val path = request.requestURI
        return !isLoginPath(path) && !isCommentPath(path)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val clientIp = HttpServletRequestUtils.resolveClientIp(request)
        val path = request.requestURI

        val exceeded = when {
            isLoginPath(path) -> loginLimiter[clientIp].incrementAndGet() > LOGIN_MAX_PER_MINUTE
            isCommentPath(path) -> commentLimiter[clientIp].incrementAndGet() > COMMENT_MAX_PER_MINUTE
            else -> false
        }

        if (exceeded) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write(
                """{"result":"ERROR","data":null,"error":{"code":"COMMON_003","message":"Too many requests"}}""",
            )
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isLoginPath(path: String): Boolean = path == "/api/v1/users/login"

    private fun isCommentPath(path: String): Boolean =
        path.matches(Regex("^/api/v1/posts/\\d+/comments$"))
}
