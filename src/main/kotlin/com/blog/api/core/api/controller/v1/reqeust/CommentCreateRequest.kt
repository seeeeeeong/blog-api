package com.blog.api.core.api.controller.v1.reqeust

import com.blog.api.core.domain.CommentCreate
import com.blog.api.core.domain.OAuthUser
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType

data class CommentCreateRequest(
    val content: String,
    val parentId: Long? = null
) {
    fun toCommentCreate(postId: Long, oauthUser: OAuthUser): CommentCreate {
        if (content.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        if (content.length > 1000) throw CoreException(ErrorType.INVALID_INPUT)

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
