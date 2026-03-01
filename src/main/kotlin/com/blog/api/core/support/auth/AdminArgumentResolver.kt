package com.blog.api.core.support.auth

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AdminArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(Admin::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw CoreException(ErrorType.UNAUTHORIZED)

        if (authentication.authorities.none { it.authority == "ROLE_ADMIN" }) {
            throw CoreException(ErrorType.FORBIDDEN)
        }

        return authentication.principal as? Long
            ?: throw CoreException(ErrorType.UNAUTHORIZED)
    }
}
