package com.blog.api.core.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class RefreshToken(
    val tokenId: String,
    val userId: Long,
    val familyId: String,
    val isUsed: Boolean = false,
    val expiresAt: Instant
)
