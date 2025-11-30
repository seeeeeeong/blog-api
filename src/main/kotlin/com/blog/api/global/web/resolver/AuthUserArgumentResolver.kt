package com.blog.api.global.web.resolver

import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import com.blog.api.global.security.JwtProvider
import com.blog.api.global.web.annotation.AuthUser
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthUserArgumentResolver(
    private val jwtProvider: JwtProvider
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthUser::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Long {
        val authorization = webRequest.getHeader("Authorization")
            ?: throw CustomException(ErrorCode.UNAUTHORIZED)

        if (authorization.startsWith("Bearer ")) {
            val token = authorization.substring(7)

            if (jwtProvider.validateToken(token)) {
                return jwtProvider.getUserIdFromToken(token)
            }
        }

        throw CustomException(ErrorCode.INVALID_TOKEN)
    }
}