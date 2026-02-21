package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.UserToken

data class UserTokenResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        fun of(userToken: UserToken) = UserTokenResponse(
            accessToken = userToken.accessToken,
            refreshToken = userToken.refreshToken,
        )
    }
}
