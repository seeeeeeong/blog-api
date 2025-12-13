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

        if (isLocal()) logInfo("HTTP Request", request.method, request.requestURI, request.remoteAddr)
        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        val duration = getDuration(request) ?: return
        val method = request.method
        val uri = request.requestURI
        val status = response.status

        when {
            isLocal() ->
                logger.info("HTTP Response: method=$method, uri=$uri, status=$status, duration=${duration}ms")
            isSlowRequest(duration) ->
                logger.warn("Slow request: method=$method, uri=$uri, status=$status, duration=${duration}ms")
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

    private fun getDuration(request: HttpServletRequest): Long? =
        (request.getAttribute(START_TIME) as? Long)?.let { System.currentTimeMillis() - it }

    private fun isLocal() = profile != "prod"

    private fun isSlowRequest(duration: Long) = duration > SLOW_REQUEST_THRESHOLD

    private fun logInfo(prefix: String, method: String, uri: String, remoteAddr: String?) {
        logger.info("$prefix: method=$method, uri=$uri, remoteAddr=$remoteAddr")
    }
}
