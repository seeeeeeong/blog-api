package com.blog.api.core.integration.oauth

import com.blog.api.core.support.security.JwtProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class GitHubOAuthService(
    private val jwtProvider: JwtProvider,
    private val gitHubOAuthClient: GitHubOAuthClient,
    @Value("\${spring.security.oauth2.client.registration.github.client-id}")
    private val clientId: String,
    @Value("\${spring.security.oauth2.client.registration.github.client-secret}")
    private val clientSecret: String
) {

    fun getAccessToken(code: String): String {
        val token = gitHubOAuthClient.exchangeCodeForToken(code, clientId, clientSecret)
        return token.accessToken
    }

    fun getGitHubUser(accessToken: String): GitHubOAuthUser {
        return gitHubOAuthClient.getUserInfo(accessToken)
    }

    fun generateCommentToken(githubUser: GitHubOAuthUser): String {
        return jwtProvider.generateGitHubAccessToken(
            githubId = githubUser.id,
            githubUsername = githubUser.login,
            githubAvatarUrl = githubUser.avatarUrl
        )
    }

    fun verifyToken(token: String): Boolean {
        return jwtProvider.validateToken(token)
    }
}
