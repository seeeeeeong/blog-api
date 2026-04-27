package com.blog.api.core.support.web

import com.blog.api.core.support.properties.BlogProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
        private val log = KotlinLogging.logger {}
        private const val REQUEST_ID_LENGTH = 8
        private const val NANOS_PER_MILLI = 1_000_000
    }

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
                log.warn {
                    "Slow request: method=${request.method}, uri=${request.requestURI}, " +
                        "status=${response.status}, elapsedMs=$elapsedMs"
                }
            }
            MDC.clear()
        }
    }
}
