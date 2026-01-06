package com.blog.api.core.domain

import com.blog.api.enums.UserRole
import java.time.LocalDateTime

data class User(
    val id: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String,
    val role: UserRole,
    val createdAt: LocalDateTime
)
