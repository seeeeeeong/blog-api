package com.blog.api.core.api.controller.v1.request

import com.blog.api.core.domain.CommentCreate
import com.blog.api.core.domain.OAuthUser
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CommentCreateRequest(
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String,
    val parentId: Long? = null
) {
    fun toCommentCreate(postId: Long, oauthUser: OAuthUser): CommentCreate {
        return CommentCreate(
            postId = postId,
            oauthId = oauthUser.id.toString(),
            oauthUsername = oauthUser.login,
            oauthAvatarUrl = oauthUser.avatarUrl,
            parentId = parentId ?: 0L,
            content = content
        )
    }
}
