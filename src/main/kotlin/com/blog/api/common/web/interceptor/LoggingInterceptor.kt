package com.blog.api.common.web.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

@Component
class LoggingInterceptor(
    @Value("\${spring.profiles.active:local}") private val profile: String
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)
    private val isDev = profile != "prod"

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        request.setAttribute("startTime", System.currentTimeMillis())

        if (isDev) {
            logger.info(
                "HTTP Request: method={}, uri={}, remoteAddr={}",
                request.method,
                request.requestURI,
                request.remoteAddr
            )
        }

        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        val startTime = request.getAttribute("startTime") as? Long ?: return
        val duration = System.currentTimeMillis() - startTime

        when {
            isDev -> logger.info(
                "HTTP Response: method={}, uri={}, status={}, duration={}ms",
                request.method,
                request.requestURI,
                response.status,
                duration
            )
            duration > 3000 -> logger.warn(
                "Slow request: method={}, uri={}, status={}, duration={}ms",
                request.method,
                request.requestURI,
                response.status,
                duration
            )
        }
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        ex?.let {
            logger.error(
                "Request failed: method={}, uri={}, error={}",
                request.method,
                request.requestURI,
                it.message,
                it
            )
        }
    }
}
