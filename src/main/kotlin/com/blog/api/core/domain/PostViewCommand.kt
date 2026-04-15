package com.blog.api.core.domain

import com.blog.api.core.enum.UserRole

data class PostViewCommand(
    val postId: Long,
    val clientIp: String,
    val viewerUserId: Long? = null,
    val viewerRole: UserRole? = null,
)
