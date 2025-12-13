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

    @Around("execution(* com.blog.api.domain..service..*(..))")
    fun logServiceMethods(joinPoint: ProceedingJoinPoint): Any? {
        val className = joinPoint.signature.declaringTypeName.substringAfterLast(".")
        val methodName = joinPoint.signature.name
        val args = joinPoint.args.joinToString(", ") { it?.toString() ?: "null" }

        logger.debug("Calling {}.{}() with args: [{}]", className, methodName, args)

        val startTime = System.currentTimeMillis()

        return runCatching {
            val result = joinPoint.proceed()
            val duration = System.currentTimeMillis() - startTime

            logger.debug("{}.{}() completed in {}ms", className, methodName, duration)

            if (duration > 1000) {
                logger.warn("Slow service method: {}.{}() took {}ms", className, methodName, duration)
            }

            result
        }.onFailure { e ->
            val duration = System.currentTimeMillis() - startTime
            logger.error(
                "{}.{}() failed after {}ms with error: {}",
                className,
                methodName,
                duration,
                e.message,
                e
            )
        }.getOrThrow()
    }
}
