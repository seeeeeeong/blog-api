package com.blog.api.common.web.resolver

import com.blog.api.common.exception.CustomException
import com.blog.api.common.exception.ErrorCode
import com.blog.api.common.security.JwtProvider
import com.blog.api.common.web.annotation.GitHubAuth
import com.blog.api.common.web.dto.GitHubUser
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class GitHubAuthArgumentResolver(
    private val jwtProvider: JwtProvider
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(GitHubAuth::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): GitHubUser {
        val authorization = webRequest.getHeader("Authorization")
            ?: throw CustomException(ErrorCode.UNAUTHORIZED)

        val token = authorization.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)
            ?: throw CustomException(ErrorCode.INVALID_TOKEN)

        val isValidToken = jwtProvider.validateToken(token)
        if (isValidToken == false) {
            throw CustomException(ErrorCode.INVALID_TOKEN)
        }

        return GitHubUser(
            githubId = jwtProvider.getGitHubIdFromToken(token),
            githubUsername = jwtProvider.getGitHubUsernameFromToken(token),
            githubAvatarUrl = jwtProvider.getGitHubAvatarUrlFromToken(token)
        )
    }
}