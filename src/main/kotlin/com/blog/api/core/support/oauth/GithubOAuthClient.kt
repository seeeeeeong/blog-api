package com.blog.api.core.support.oauth

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.OAuthUserProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Component
class GithubOAuthClient(
    private val restClient: RestClient,
    private val properties: OAuthUserProperties,
) {
    fun fetchAccessToken(code: String): String {
        val params = LinkedMultiValueMap<String, String>().apply {
            add("client_id", properties.clientId)
            add("client_secret", properties.clientSecret)
            add("code", code)
            if (properties.callbackUrl.isNotBlank()) add("redirect_uri", properties.callbackUrl)
        }
        val response = restClient.post()
            .uri(properties.tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept(MediaType.APPLICATION_JSON)
            .body(params)
            .retrieve()
            .body(GithubTokenResponse::class.java)
        return response?.accessToken ?: throw CoreException(ErrorType.INVALID_TOKEN)
    }

    fun fetchUser(accessToken: String): GithubUserResponse {
        return restClient.get()
            .uri(properties.userApiUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(GithubUserResponse::class.java) ?: throw CoreException(ErrorType.USER_NOT_FOUND)
    }
}
