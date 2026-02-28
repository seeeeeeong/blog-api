package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.api.controller.v1.response.OAuthTokenResponse
import com.blog.api.core.api.controller.v1.response.OAuthUserResponse
import com.blog.api.core.support.properties.OAuthUserProperties
import com.blog.api.core.support.security.JwtProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit

@Service
class OAuthService(
    private val jwtProvider: JwtProvider,
    private val restClient: RestClient,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val properties: OAuthUserProperties,
) {
    companion object {
        private const val STATE_TTL_SECONDS = 600L
        private const val EXCHANGE_CODE_TTL_SECONDS = 120L
        private const val STATE_KEY_PREFIX = "oauth:state:"
        private const val EXCHANGE_KEY_PREFIX = "oauth:exchange:"
    }

    private val secureRandom = SecureRandom()

    fun createAuthorizationUrl(): String {
        val state = generateState()
        val builder = UriComponentsBuilder.fromUriString(properties.authorizationUri)
            .queryParam("client_id", properties.clientId)
            .queryParam("response_type", "code")
            .queryParam("state", state)
            .queryParam("scope", properties.scope)
        appendCallbackUrl(builder)
        return builder.build().encode().toUriString()
    }

    fun handleCallback(code: String, state: String): String {
        validateCallbackInput(code, state)
        validateAndConsumeState(state)

        val githubToken = exchangeCodeForToken(code)
        val oauthUser = getUserInfo(githubToken)
        val oauthLogin = OAuthLogin(token = generateCommentToken(oauthUser), user = oauthUser)

        return storeExchangeCode(oauthLogin)
    }

    fun exchangeCode(code: String): OAuthLogin {
        validateExchangeCodeInput(code)
        val key = exchangeCodeKey(code)
        val payloadJson = redisTemplate.opsForValue().get(key)
            ?: throw CoreException(ErrorType.OAUTH_CODE_INVALID)
        redisTemplate.delete(key)
        return objectMapper.readValue(payloadJson)
    }

    fun verifyToken(token: String): Boolean = jwtProvider.validateToken(token)

    private fun validateCallbackInput(code: String, state: String) {
        val hasCode = code.isNotBlank()
        val hasState = state.isNotBlank()
        if (hasCode && hasState) {
            return
        }
        throw CoreException(ErrorType.INVALID_INPUT)
    }

    private fun validateExchangeCodeInput(code: String) {
        if (code.isNotBlank()) {
            return
        }
        throw CoreException(ErrorType.INVALID_INPUT)
    }

    private fun appendCallbackUrl(builder: UriComponentsBuilder) {
        val callbackUrl = properties.callbackUrl
        if (callbackUrl.isBlank()) {
            return
        }
        builder.queryParam("redirect_uri", callbackUrl)
    }

    private fun generateState(): String {
        val state = generateRandomToken()
        redisTemplate.opsForValue().set(stateKey(state), "1", STATE_TTL_SECONDS, TimeUnit.SECONDS)
        return state
    }

    private fun validateAndConsumeState(state: String) {
        val key = stateKey(state)
        redisTemplate.opsForValue().get(key)
            ?: throw CoreException(ErrorType.OAUTH_STATE_INVALID)
        redisTemplate.delete(key)
    }

    private fun storeExchangeCode(oauthLogin: OAuthLogin): String {
        val exchangeCode = generateRandomToken()
        redisTemplate.opsForValue().set(
            exchangeCodeKey(exchangeCode),
            objectMapper.writeValueAsString(oauthLogin),
            EXCHANGE_CODE_TTL_SECONDS,
            TimeUnit.SECONDS,
        )
        return exchangeCode
    }

    private fun generateCommentToken(oauthUser: OAuthUser): String {
        return jwtProvider.generateOAuthAccessToken(
            oauthId = oauthUser.id,
            oauthUsername = oauthUser.login,
            oauthAvatarUrl = oauthUser.avatarUrl,
        )
    }

    private fun exchangeCodeForToken(code: String): String {
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
            .body(OAuthTokenResponse::class.java)
        return response?.accessToken ?: throw CoreException(ErrorType.INVALID_TOKEN)
    }

    private fun getUserInfo(accessToken: String): OAuthUser {
        val body = restClient.get()
            .uri(properties.userApiUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(OAuthUserResponse::class.java) ?: throw CoreException(ErrorType.USER_NOT_FOUND)
        return OAuthUser(
            id = body.id,
            login = body.login,
            avatarUrl = body.avatarUrl,
            name = body.name,
            email = body.email,
        )
    }

    private fun generateRandomToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun stateKey(state: String): String = "$STATE_KEY_PREFIX$state"

    private fun exchangeCodeKey(code: String): String = "$EXCHANGE_KEY_PREFIX$code"
}
