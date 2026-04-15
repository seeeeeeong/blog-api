package com.blog.api.core.support.web

import com.blog.api.core.support.properties.BlogProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MdcLoggingFilter(
    private val blogProperties: BlogProperties,
) : OncePerRequestFilter() {

    companion object {
        private const val REQUEST_ID_LENGTH = 8
        private const val NANOS_PER_MILLI = 1_000_000
    }

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = UUID.randomUUID().toString().take(REQUEST_ID_LENGTH)
        val startNs = System.nanoTime()
        MDC.put("requestId", requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            val elapsedMs = (System.nanoTime() - startNs) / NANOS_PER_MILLI
            if (elapsedMs >= blogProperties.slowRequestThresholdMs) {
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
