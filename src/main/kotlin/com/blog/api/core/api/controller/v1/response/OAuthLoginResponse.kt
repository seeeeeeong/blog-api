package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.OAuthLogin
import com.blog.api.core.domain.OAuthUser

data class OAuthLoginResponse(
    val token: String,
    val user: OAuthUser
) {
    companion object {
        fun of(result: OAuthLogin) = OAuthLoginResponse(
            token = result.token,
            user = result.user
        )
    }
}
