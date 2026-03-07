package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.oauth.GithubOAuthClient
import com.blog.api.core.support.properties.OAuthUserProperties
import com.blog.api.core.support.security.JwtProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit

@Service
class OAuthService(
    private val jwtProvider: JwtProvider,
    private val githubOAuthClient: GithubOAuthClient,
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

    fun createAuthorization(): OAuthAuthorization {
        val state = generateState()
        val builder = UriComponentsBuilder.fromUriString(properties.authorizationUri)
            .queryParam("client_id", properties.clientId)
            .queryParam("response_type", "code")
            .queryParam("state", state)
            .queryParam("scope", properties.scope)
        properties.callbackUrl.takeIf { it.isNotBlank() }?.let { builder.queryParam("redirect_uri", it) }
        return OAuthAuthorization(
            url = builder.build().encode().toUriString(),
            state = state,
            stateTtlSeconds = STATE_TTL_SECONDS,
        )
    }

    fun handleCallback(code: String, state: String, stateCookie: String?): String {
        if (code.isBlank() || state.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        if (stateCookie.isNullOrBlank() || stateCookie != state) throw CoreException(ErrorType.OAUTH_STATE_INVALID)
        validateAndConsumeState(state)

        val githubToken = githubOAuthClient.fetchAccessToken(code)
        val githubUser = githubOAuthClient.fetchUser(githubToken)
        val oauthUser = OAuthUser(
            id = githubUser.id,
            login = githubUser.login,
            avatarUrl = githubUser.avatarUrl,
            name = githubUser.name,
            email = githubUser.email,
        )
        val oauthLogin = OAuthLogin(token = generateCommentToken(oauthUser), user = oauthUser)
        return storeExchangeCode(oauthLogin)
    }

    fun exchangeCode(code: String): OAuthLogin {
        if (code.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        val payloadJson = getAndDelete(exchangeCodeKey(code), "oauth.exchange.get-and-delete")
            ?: throw CoreException(ErrorType.OAUTH_CODE_INVALID)
        return objectMapper.readValue(payloadJson)
    }

    fun verifyToken(token: String): Boolean = jwtProvider.validateOAuthCommentToken(token)

    private fun generateState(): String {
        val state = generateRandomToken()
        set(stateKey(state), "1", STATE_TTL_SECONDS, "oauth.state.set")
        return state
    }

    private fun validateAndConsumeState(state: String) {
        getAndDelete(stateKey(state), "oauth.state.get-and-delete")
            ?: throw CoreException(ErrorType.OAUTH_STATE_INVALID)
    }

    private fun storeExchangeCode(oauthLogin: OAuthLogin): String {
        val exchangeCode = generateRandomToken()
        set(
            exchangeCodeKey(exchangeCode),
            objectMapper.writeValueAsString(oauthLogin),
            EXCHANGE_CODE_TTL_SECONDS,
            "oauth.exchange.set",
        )
        return exchangeCode
    }

    private fun generateCommentToken(oauthUser: OAuthUser): String =
        jwtProvider.generateOAuthAccessToken(
            oauthId = oauthUser.id,
            oauthUsername = oauthUser.login,
            oauthAvatarUrl = oauthUser.avatarUrl,
        )

    private fun generateRandomToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun stateKey(state: String): String = "$STATE_KEY_PREFIX$state"

    private fun exchangeCodeKey(code: String): String = "$EXCHANGE_KEY_PREFIX$code"

    private fun set(key: String, value: String, ttlSeconds: Long, action: String) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw redisUnavailable(action, e)
        }
    }

    private fun getAndDelete(key: String, action: String): String? {
        return try {
            redisTemplate.opsForValue().getAndDelete(key)
        } catch (e: Exception) {
            throw redisUnavailable(action, e)
        }
    }

    private fun redisUnavailable(action: String, cause: Exception): CoreException {
        return CoreException(
            errorType = ErrorType.REDIS_UNAVAILABLE,
            message = "Redis unavailable during $action",
            cause = cause,
        )
    }
}
