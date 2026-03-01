package com.blog.api.core.support.oauth

import com.fasterxml.jackson.annotation.JsonProperty

data class GithubTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
)
