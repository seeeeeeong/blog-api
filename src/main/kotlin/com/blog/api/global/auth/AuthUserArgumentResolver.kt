package com.blog.api.global.auth

import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import com.blog.api.global.security.JwtProvider
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

        if (!authorization.startsWith("Bearer ")) {
            throw CustomException(ErrorCode.INVALID_TOKEN)
        }

        val token = authorization.substring(7)
        
        if (!jwtProvider.validateToken(token)) {
            throw CustomException(ErrorCode.INVALID_TOKEN)
        }

        return jwtProvider.getUserIdFromToken(token)
    }
}