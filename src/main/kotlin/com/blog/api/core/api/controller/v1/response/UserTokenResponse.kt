package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.UserToken

data class UserTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val refreshTokenId: String,
    val user: UserResponse
) {
    companion object {
        fun of(userToken: UserToken): UserTokenResponse {
            return UserTokenResponse(
                accessToken = userToken.accessToken,
                refreshToken = userToken.refreshToken,
                refreshTokenId = userToken.refreshTokenId,
                user = UserResponse.of(userToken.user)
            )
        }
    }
}
