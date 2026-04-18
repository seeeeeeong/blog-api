package com.blog.api.core.domain.user

import com.blog.api.core.enum.UserRole
import java.time.LocalDateTime

data class User(
    val id: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String,
    val role: UserRole,
    val createdAt: LocalDateTime,
)
