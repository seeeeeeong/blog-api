package com.blog.api.core.support.web.resolver

import com.blog.api.core.integration.oauth.GitHubOAuthUser
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.security.JwtProvider
import com.blog.api.core.support.util.BearerTokenExtractor
import com.blog.api.core.support.web.annotation.GitHubAuth
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class GitHubAuthArgumentResolver(
    private val jwtProvider: JwtProvider,
    private val bearerTokenExtractor: BearerTokenExtractor
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(GitHubAuth::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): GitHubOAuthUser {
        val authHeader = webRequest.getHeader("Authorization")
        val token = bearerTokenExtractor.extractOrThrow(authHeader)
            .takeIf { jwtProvider.validateToken(it) }
            ?: throw CoreException(ErrorType.INVALID_TOKEN)

        val githubId = jwtProvider.getGitHubIdFromToken(token).toLongOrNull()
            ?: throw CoreException(ErrorType.INVALID_TOKEN)

        return GitHubOAuthUser(
            id = githubId,
            login = jwtProvider.getGitHubUsernameFromToken(token),
            avatarUrl = jwtProvider.getGitHubAvatarUrlFromToken(token),
            name = null,
            email = null
        )
    }
}
