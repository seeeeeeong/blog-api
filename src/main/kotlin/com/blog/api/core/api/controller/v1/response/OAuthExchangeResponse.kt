package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.OAuthExchangeResult

data class OAuthExchangeResponse(
    val token: String,
    val user: OAuthUserProfileResponse
) {
    companion object {
        fun of(result: OAuthExchangeResult): OAuthExchangeResponse {
            return OAuthExchangeResponse(
                token = result.token,
                user = OAuthUserProfileResponse.of(result.user)
            )
        }
    }
}
