package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.Token

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val refreshTokenId: String,
    val user: UserResponse
) {
    companion object {
        fun of(token: Token): TokenResponse {
            return TokenResponse(
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
                refreshTokenId = token.refreshTokenId,
                user = UserResponse.of(token.user)
            )
        }
    }
}
