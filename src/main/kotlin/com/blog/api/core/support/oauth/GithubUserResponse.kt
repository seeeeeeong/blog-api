package com.blog.api.core.support.oauth

import com.fasterxml.jackson.annotation.JsonProperty

data class GithubUserResponse(
    val id: Long,
    val login: String,
    @JsonProperty("avatar_url")
    val avatarUrl: String = "",
    val name: String = "",
    val email: String? = null,
)
