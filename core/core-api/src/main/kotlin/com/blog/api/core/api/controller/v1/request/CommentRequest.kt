package com.blog.api.core.api.controller.v1.request

import com.blog.api.core.domain.CommentCreate
import com.blog.api.core.domain.CommentUpdate
import com.blog.api.core.integration.oauth.GitHubOAuthUser
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType

data class CommentCreateRequest(
    val content: String,
    val parentId: Long? = null
) {
    fun toCommentCreate(postId: Long, githubUser: GitHubOAuthUser): CommentCreate {
        if (content.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        if (content.length > 1000) throw CoreException(ErrorType.INVALID_INPUT)

        return CommentCreate(
            postId = postId,
            githubId = githubUser.id.toString(),
            githubUsername = githubUser.login,
            githubAvatarUrl = githubUser.avatarUrl ?: "",
            parentId = parentId ?: 0L,
            content = content
        )
    }
}

data class CommentUpdateRequest(
    val content: String
) {
    fun toCommentUpdate(): CommentUpdate {
        if (content.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        if (content.length > 1000) throw CoreException(ErrorType.INVALID_INPUT)
        return CommentUpdate(content = content)
    }
}
