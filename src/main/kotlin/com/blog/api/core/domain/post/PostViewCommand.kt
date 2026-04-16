package com.blog.api.core.domain.post

import com.blog.api.core.enum.UserRole
import jakarta.servlet.http.HttpServletResponse

data class PostViewCommand(
    val postId: Long,
    val hasViewedCookie: Boolean,
    val response: HttpServletResponse,
    val viewerUserId: Long? = null,
    val viewerRole: UserRole? = null,
)
