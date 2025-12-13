package com.blog.api.infrastructure.oauth.service

import com.blog.api.common.exception.CustomException
import com.blog.api.common.exception.ErrorCode
import com.blog.api.common.security.JwtProvider
import com.blog.api.infrastructure.oauth.dto.CommentAuthResponse
import com.blog.api.infrastructure.oauth.dto.GitHubUserResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Service
class GitHubOAuthService(
    private val jwtProvider: JwtProvider,
    private val restTemplate: RestTemplate,
    @Value("\${spring.security.oauth2.client.registration.github.client-id}")
    private val clientId: String,
    @Value("\${spring.security.oauth2.client.registration.github.client-secret}")
    private val clientSecret: String,
    @Value("\${oauth.github.redirect-url}")
    private val redirectUrl: String
) {

    companion object {
        private const val GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token"
        private const val GITHUB_USER_API_URL = "https://api.github.com/user"
        private const val PARAM_CLIENT_ID = "client_id"
        private const val PARAM_CLIENT_SECRET = "client_secret"
        private const val PARAM_CODE = "code"
        private const val RESPONSE_ACCESS_TOKEN = "access_token"
        private const val BODY_KEY_ID = "id"
        private const val BODY_KEY_LOGIN = "login"
        private const val BODY_KEY_AVATAR_URL = "avatar_url"
        private const val BODY_KEY_NAME = "name"
        private const val BODY_KEY_EMAIL = "email"
    }

    fun getAccessToken(code: String): String {
        val params = createTokenRequestParams(code)
        val request = HttpEntity(params, createJsonHeaders())

        val responseBody = restTemplate.postForEntity(GITHUB_TOKEN_URL, request, Map::class.java).body
            ?: throw CustomException(ErrorCode.INVALID_TOKEN)

        return (responseBody[RESPONSE_ACCESS_TOKEN] as? String)
            .takeIf { it?.isNotBlank() == true }
            ?: throw CustomException(ErrorCode.INVALID_TOKEN)
    }


    fun getGitHubUser(accessToken: String): GitHubUserResponse {
        val headers = createAuthHeaders(accessToken)
        val request = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(GITHUB_USER_API_URL, HttpMethod.GET, request, Map::class.java)
        val body = response.body ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        return parseGitHubUserResponse(body)
    }

    private fun createTokenRequestParams(code: String): LinkedMultiValueMap<String, String> {
        val params = LinkedMultiValueMap<String, String>()
        params.add(PARAM_CLIENT_ID, clientId)
        params.add(PARAM_CLIENT_SECRET, clientSecret)
        params.add(PARAM_CODE, code)
        return params
    }

    private fun createJsonHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        return headers
    }

    private fun createAuthHeaders(accessToken: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.setBearerAuth(accessToken)
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        return headers
    }

    private fun parseGitHubUserResponse(body: Map<*, *>): GitHubUserResponse {
        return GitHubUserResponse(
            id = (body[BODY_KEY_ID] as Number).toLong(),
            login = body[BODY_KEY_LOGIN] as String,
            avatarUrl = body[BODY_KEY_AVATAR_URL] as? String,
            name = body[BODY_KEY_NAME] as? String,
            email = body[BODY_KEY_EMAIL] as? String
        )
    }

    fun generateCommentToken(githubUserResponse: GitHubUserResponse): CommentAuthResponse {
        val commentToken = jwtProvider.generateGitHubAccessToken(
            githubId = githubUserResponse.id,
            githubUsername = githubUserResponse.login,
            githubAvatarUrl = githubUserResponse.avatarUrl
        )

        return CommentAuthResponse(
            commentToken = commentToken,
            githubId = githubUserResponse.id.toString(),
            githubUsername = githubUserResponse.login,
            githubAvatarUrl = githubUserResponse.avatarUrl
        )
    }

    fun verifyToken(token: String): Boolean {
        return jwtProvider.validateToken(token)
    }
}