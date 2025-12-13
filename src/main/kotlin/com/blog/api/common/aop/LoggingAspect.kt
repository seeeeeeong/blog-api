package com.blog.api.common.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class LoggingAspect {

    private val logger = LoggerFactory.getLogger(LoggingAspect::class.java)
    private companion object {
        const val SLOW_METHOD_THRESHOLD_MS = 1000L
    }

    @Around("execution(* com.blog.api.domain..service..*(..))")
    fun logServiceMethods(joinPoint: ProceedingJoinPoint): Any? {
        val className = joinPoint.signature.declaringTypeName.substringAfterLast(".")
        val methodName = joinPoint.signature.name
        val args = joinPoint.args.joinToString(", ") { it?.toString() ?: "null" }

        logger.debug("Calling {}.{}() with args: [{}]", className, methodName, args)
        val startTime = System.currentTimeMillis()

        return runCatching {
            val result = joinPoint.proceed()
            logExecutionTime(className, methodName, startTime)
            result
        }.onFailure { e ->
            logException(className, methodName, startTime, e)
        }.getOrThrow()
    }

    private fun logExecutionTime(className: String, methodName: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        logger.debug("{}.{}() completed in {}ms", className, methodName, duration)
        duration.takeIf { it > SLOW_METHOD_THRESHOLD_MS }
            ?.let { logger.warn("Slow service method: {}.{}() took {}ms", className, methodName, it) }
    }

    private fun logException(className: String, methodName: String, startTime: Long, e: Throwable) {
        val duration = System.currentTimeMillis() - startTime
        logger.error(
            "{}.{}() failed after {}ms with error: {}",
            className,
            methodName,
            duration,
            e.message,
            e
        )
    }
}
