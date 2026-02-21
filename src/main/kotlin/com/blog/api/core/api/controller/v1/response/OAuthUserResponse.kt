package com.blog.api.core.api.controller.v1.response

import com.fasterxml.jackson.annotation.JsonProperty

data class OAuthUserResponse(
    val id: Long,
    val login: String,
    @JsonProperty("avatar_url")
    val avatarUrl: String = "",
    val name: String = "",
    val email: String = "",
)
