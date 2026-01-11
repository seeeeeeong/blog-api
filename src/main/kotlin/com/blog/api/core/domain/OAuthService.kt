package com.blog.api.core.domain

import com.blog.api.core.support.security.JwtProvider
import com.blog.api.core.support.connector.OAuthGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class OAuthService(
    private val jwtProvider: JwtProvider,
    private val oauthGateway: OAuthGateway,
    @param:Value("\${spring.security.oauth2.client.registration.github.client-id}")
    private val clientId: String,
    @param:Value("\${spring.security.oauth2.client.registration.github.client-secret}")
    private val clientSecret: String,
) {

    fun getAccessToken(code: String): String {
        return oauthGateway.exchangeCodeForToken(code, clientId, clientSecret).accessToken
    }

    fun getOAuthUser(accessToken: String): OAuthUser {
        return oauthGateway.getUserInfo(accessToken)
    }

    fun generateCommentToken(oauthUser: OAuthUser): String {
        return jwtProvider.generateOAuthAccessToken(
            oauthId = oauthUser.id,
            oauthUsername = oauthUser.login,
            oauthAvatarUrl = oauthUser.avatarUrl,
        )
    }

    fun verifyToken(token: String): Boolean {
        return jwtProvider.validateToken(token)
    }
}
