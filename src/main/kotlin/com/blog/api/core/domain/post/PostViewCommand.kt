package com.blog.api.core.domain.post

import com.blog.api.core.enum.UserRole

data class PostViewCommand(
    val postId: Long,
    val hasViewedCookie: Boolean,
    val viewerUserId: Long? = null,
    val viewerRole: UserRole? = null,
)
