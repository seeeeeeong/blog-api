package com.blog.api.core.domain

import com.blog.api.core.support.connector.OAuthCodeStore
import com.blog.api.core.support.connector.OAuthGateway
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.security.JwtProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Service
class OAuthService(
    private val jwtProvider: JwtProvider,
    private val oauthGateway: OAuthGateway,
    private val oauthCodeStore: OAuthCodeStore,
    private val objectMapper: ObjectMapper,
    @param:Value("\${spring.security.oauth2.client.registration.github.client-id}")
    private val clientId: String,
    @param:Value("\${spring.security.oauth2.client.registration.github.client-secret}")
    private val clientSecret: String,
    @param:Value("\${spring.security.oauth2.client.provider.github.authorization-uri}")
    private val authorizationUri: String,
    @param:Value("\${oauth.github.scope:read:user user:email}")
    private val scope: String,
    @param:Value("\${oauth.github.callback-url:}")
    private val callbackUrl: String
) {

    private val secureRandom = SecureRandom()

    fun generateCommentToken(oauthUser: OAuthUser): String {
        return jwtProvider.generateOAuthAccessToken(
            oauthId = oauthUser.id,
            oauthUsername = oauthUser.login,
            oauthAvatarUrl = oauthUser.avatarUrl,
        )
    }

    fun createAuthorizationUrl(): String {
        val state = generateRandomToken()
        val codeVerifier = generateRandomToken()
        val codeChallenge = sha256Base64Url(codeVerifier)
        oauthCodeStore.saveState(state, codeVerifier, STATE_TTL_SECONDS)

        val scopeParam = normalizeScopes(scope)
        val builder = UriComponentsBuilder.fromUriString(authorizationUri)
            .queryParam("client_id", clientId)
            .queryParam("response_type", "code")
            .queryParam("state", state)
            .queryParam("scope", scopeParam)
            .queryParam("code_challenge", codeChallenge)
            .queryParam("code_challenge_method", "S256")

        if (callbackUrl.isNotBlank()) {
            builder.queryParam("redirect_uri", callbackUrl)
        }

        return builder.build().encode().toUriString()
    }

    fun handleCallback(code: String, state: String): String {
        if (code.isBlank() || state.isBlank()) {
            throw CoreException(ErrorType.INVALID_INPUT)
        }
        val codeVerifier = oauthCodeStore.consumeState(state)
            ?: throw CoreException(ErrorType.OAUTH_STATE_INVALID)

        val accessToken = oauthGateway.exchangeCodeForToken(
            code = code,
            clientId = clientId,
            clientSecret = clientSecret,
            codeVerifier = codeVerifier,
            redirectUri = callbackUrl.takeIf { it.isNotBlank() }
        ).accessToken

        val oauthUser = oauthGateway.getUserInfo(accessToken)
        val commentToken = generateCommentToken(oauthUser)

        val exchangeCode = generateRandomToken()
        val payload = OAuthExchangeResult(token = commentToken, user = oauthUser)
        val payloadJson = objectMapper.writeValueAsString(payload)
        oauthCodeStore.saveExchangeCode(exchangeCode, payloadJson, EXCHANGE_CODE_TTL_SECONDS)

        return exchangeCode
    }

    fun exchangeCode(code: String): OAuthExchangeResult {
        if (code.isBlank()) {
            throw CoreException(ErrorType.INVALID_INPUT)
        }
        val payloadJson = oauthCodeStore.consumeExchangeCode(code)
            ?: throw CoreException(ErrorType.OAUTH_CODE_INVALID)
        return objectMapper.readValue(payloadJson)
    }

    fun verifyToken(token: String): Boolean {
        return jwtProvider.validateToken(token)
    }

    private fun generateRandomToken(size: Int = 32): String {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256Base64Url(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun normalizeScopes(raw: String): String {
        return raw
            .trim()
            .removePrefix("[")
            .removeSuffix("]")
            .split(",", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    companion object {
        private const val STATE_TTL_SECONDS = 600L
        private const val EXCHANGE_CODE_TTL_SECONDS = 120L
    }
}
