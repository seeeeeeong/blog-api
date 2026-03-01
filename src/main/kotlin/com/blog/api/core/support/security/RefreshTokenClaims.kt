package com.blog.api.core.support.security

data class RefreshTokenClaims(val tokenId: String, val userId: Long, val roleName: String)
