package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.OAuthUser

data class OAuthUserProfileResponse(
    val id: Long,
    val login: String,
    val avatarUrl: String,
    val name: String,
    val email: String
) {
    companion object {
        fun of(user: OAuthUser): OAuthUserProfileResponse {
            return OAuthUserProfileResponse(
                id = user.id,
                login = user.login,
                avatarUrl = user.avatarUrl,
                name = user.name,
                email = user.email
            )
        }
    }
}
