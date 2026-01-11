package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.User
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun of(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl,
                createdAt = user.createdAt
            )
        }
    }
}
