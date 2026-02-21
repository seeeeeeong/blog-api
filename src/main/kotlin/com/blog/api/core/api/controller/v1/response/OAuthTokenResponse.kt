package com.blog.api.core.api.controller.v1.response

import com.fasterxml.jackson.annotation.JsonProperty

data class OAuthTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
)
