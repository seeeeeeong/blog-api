package com.blog.api.core.support.auth

import com.blog.api.core.domain.OAuthUser
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.domain.UserTokenExtractor
import com.blog.api.core.support.security.JwtProvider
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class OAuthAuthArgumentResolver(
    private val jwtProvider: JwtProvider,
    private val userTokenExtractor: UserTokenExtractor
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(OAuthAuth::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): OAuthUser {
        val authHeader = webRequest.getHeader(AUTHORIZATION_HEADER)
        val token = userTokenExtractor.extractOrThrow(authHeader)
        if (jwtProvider.validateToken(token)) {
            val oauthId = jwtProvider.getOAuthIdFromToken(token).toLongOrNull()
                ?: throw CoreException(ErrorType.INVALID_TOKEN)
            val oauthUsername = jwtProvider.getOAuthUsernameFromToken(token)
            val oauthAvatarUrl = jwtProvider.getOAuthAvatarUrlFromToken(token).orEmpty()

            return OAuthUser(
                id = oauthId,
                login = oauthUsername,
                avatarUrl = oauthAvatarUrl,
                name = "",
                email = ""
            )
        }

        throw CoreException(ErrorType.INVALID_TOKEN)
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
    }
}
