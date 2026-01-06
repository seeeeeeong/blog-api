package com.blog.api.core.integration.oauth

import com.blog.api.core.api.config.properties.GitHubProperties
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Component
class GitHubOAuthClient(
    private val restTemplate: RestTemplate,
    private val gitHubProperties: GitHubProperties
) {

    fun exchangeCodeForToken(code: String, clientId: String, clientSecret: String): GitHubOAuthToken {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("code", code)
        }

        val headers = HttpHeaders().apply {
            accept = listOf(MediaType.APPLICATION_JSON)
        }

        val request = HttpEntity(params, headers)
        val response = restTemplate.postForEntity(
            gitHubProperties.tokenUrl,
            request,
            GitHubOAuthTokenResponse::class.java
        )

        val body = response.body ?: throw CoreException(ErrorType.INVALID_TOKEN)
        return GitHubOAuthToken(
            accessToken = body.accessToken,
            tokenType = body.tokenType,
            scope = body.scope
        )
    }

    fun getUserInfo(accessToken: String): GitHubOAuthUser {
        val headers = HttpHeaders().apply {
            setBearerAuth(accessToken)
            accept = listOf(MediaType.APPLICATION_JSON)
        }

        val request = HttpEntity<Void>(headers)
        val response = restTemplate.exchange(
            gitHubProperties.userApiUrl,
            HttpMethod.GET,
            request,
            GitHubOAuthUserResponse::class.java
        )

        val body = response.body ?: throw CoreException(ErrorType.USER_NOT_FOUND)
        return GitHubOAuthUser(
            id = body.id,
            login = body.login,
            avatarUrl = body.avatarUrl,
            name = body.name,
            email = body.email
        )
    }
}
