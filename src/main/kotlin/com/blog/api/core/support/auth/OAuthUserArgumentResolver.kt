package com.blog.api.core.support.auth

import com.blog.api.core.domain.OAuthUser as OAuthUserPrincipal
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.security.JwtProvider
import com.blog.api.core.support.web.HttpServletRequestUtils
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class OAuthUserArgumentResolver(
    private val jwtProvider: JwtProvider,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(OAuthPrincipal::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): OAuthUserPrincipal {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw CoreException(ErrorType.INVALID_TOKEN)

        val token = HttpServletRequestUtils.extractBearerTokenOrThrow(request)
        val isValidToken = jwtProvider.validateToken(token)
        if (isValidToken) {
            val oauthId = jwtProvider.getOAuthIdFromToken(token).toLongOrNull()
                ?: throw CoreException(ErrorType.INVALID_TOKEN)

            return OAuthUserPrincipal(
                id = oauthId,
                login = jwtProvider.getOAuthUsernameFromToken(token),
                avatarUrl = jwtProvider.getOAuthAvatarUrlFromToken(token).orEmpty(),
                name = "",
                email = null
            )
        }

        throw CoreException(ErrorType.INVALID_TOKEN)
    }
}
