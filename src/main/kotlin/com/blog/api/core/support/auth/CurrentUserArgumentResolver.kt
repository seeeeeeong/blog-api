package com.blog.api.core.support.auth

import com.blog.api.core.enum.UserRole
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(ResolveCurrentUser::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): CurrentUser {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: return CurrentUser(userId = null, role = null)

        val userId = authentication.principal as? Long
        val role =
            if (authentication.authorities.any { it.authority == "ROLE_ADMIN" }) {
                UserRole.ADMIN
            } else {
                UserRole.USER
            }

        return CurrentUser(userId = userId, role = role)
    }
}
