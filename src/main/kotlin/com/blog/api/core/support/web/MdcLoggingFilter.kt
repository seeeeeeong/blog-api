package com.blog.api.core.support.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MdcLoggingFilter(
    @param:Value("\${blog.slow-request-threshold-ms:700}")
    private val slowRequestThresholdMs: Long,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = UUID.randomUUID().toString().take(8)
        val startNs = System.nanoTime()
        MDC.put("requestId", requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
            if (elapsedMs >= slowRequestThresholdMs) {
                log.warn(
                    "Slow request: method={}, uri={}, status={}, elapsedMs={}",
                    request.method,
                    request.requestURI,
                    response.status,
                    elapsedMs
                )
            }
            MDC.clear()
        }
    }
}
