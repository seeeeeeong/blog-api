package com.blog.api.core.support.connector

import com.blog.api.core.support.properties.OAuthProperties
import com.blog.api.core.domain.OAuthToken
import com.blog.api.core.domain.OAuthUser
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.api.controller.v1.response.OAuthTokenResponse
import com.blog.api.core.api.controller.v1.response.OAuthUserResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Component
class OAuthGateway(
    private val restTemplate: RestTemplate,
    private val oauthProperties: OAuthProperties
) {

    fun exchangeCodeForToken(code: String, clientId: String, clientSecret: String): OAuthToken {
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
            oauthProperties.tokenUrl,
            request,
            OAuthTokenResponse::class.java
        )

        val body = response.body ?: throw CoreException(ErrorType.INVALID_TOKEN)
        return OAuthTokenResponse.of(body)
    }

    fun getUserInfo(accessToken: String): OAuthUser {
        val headers = HttpHeaders().apply {
            setBearerAuth(accessToken)
            accept = listOf(MediaType.APPLICATION_JSON)
        }

        val request = HttpEntity<Void>(headers)
        val response = restTemplate.exchange(
            oauthProperties.userApiUrl,
            HttpMethod.GET,
            request,
            OAuthUserResponse::class.java
        )

        val body = response.body ?: throw CoreException(ErrorType.USER_NOT_FOUND)
        return OAuthUserResponse.of(body)
    }
}
