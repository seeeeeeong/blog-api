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
    @Value("\${spring.profiles.active:local}") profile: String
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)
    private val isDev = profile != "prod"

    companion object {
        private const val START_TIME = "START_TIME"
        private const val SLOW_REQUEST_THRESHOLD = 3000L
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        request.setAttribute(START_TIME, System.currentTimeMillis())

        if (isDev) logRequest(request)
        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        val duration = getDuration(request) ?: return

        when {
            shouldLogAllResponses() ->
                logResponse(request, response, duration)

            shouldLogSlowRequest(duration) ->
                logSlowRequest(request, response, duration)
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


    private fun logRequest(request: HttpServletRequest) {
        logger.info(
            "HTTP Request: method={}, uri={}, remoteAddr={}",
            request.method,
            request.requestURI,
            request.remoteAddr
        )
    }

    private fun logResponse(
        request: HttpServletRequest,
        response: HttpServletResponse,
        duration: Long
    ) {
        logger.info(
            "HTTP Response: method={}, uri={}, status={}, duration={}ms",
            request.method,
            request.requestURI,
            response.status,
            duration
        )
    }

    private fun logSlowRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        duration: Long
    ) {
        logger.warn(
            "Slow request: method={}, uri={}, status={}, duration={}ms",
            request.method,
            request.requestURI,
            response.status,
            duration
        )
    }

    private fun getDuration(request: HttpServletRequest): Long? {
        val startTime = request.getAttribute(START_TIME) as? Long ?: return null
        return System.currentTimeMillis() - startTime
    }

    private fun shouldLogAllResponses() = isDev

    private fun shouldLogSlowRequest(duration: Long) =
        duration > SLOW_REQUEST_THRESHOLD

}
