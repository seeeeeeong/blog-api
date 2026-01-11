package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.OAuthToken
import com.fasterxml.jackson.annotation.JsonProperty

data class OAuthTokenResponse(
    @param:JsonProperty("access_token")
    val accessToken: String,
    @param:JsonProperty("token_type")
    val tokenType: String = "bearer",
    val scope: String? = null
) {
    companion object {
        fun of(response: OAuthTokenResponse): OAuthToken {
            return OAuthToken(
                accessToken = response.accessToken,
                tokenType = response.tokenType,
                scope = response.scope.orEmpty()
            )
        }
    }
}
