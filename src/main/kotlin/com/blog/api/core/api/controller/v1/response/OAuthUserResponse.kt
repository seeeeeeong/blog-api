package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.OAuthUser

data class OAuthUserResponse(
    val id: Long,
    val login: String,
    val avatarUrl: String?,
    val name: String?,
    val email: String?
) {
    companion object {
        fun of(response: OAuthUserResponse): OAuthUser {
            return OAuthUser(
                id = response.id,
                login = response.login,
                avatarUrl = response.avatarUrl.orEmpty(),
                name = response.name.orEmpty(),
                email = response.email.orEmpty()
            )
        }
    }
}
