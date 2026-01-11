package com.blog.api.core.api.controller.v1.reqeust

import com.blog.api.core.domain.CommentUpdate
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType

data class CommentUpdateRequest(
    val content: String
) {
    fun toCommentUpdate(): CommentUpdate {
        if (content.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        if (content.length > 1000) throw CoreException(ErrorType.INVALID_INPUT)
        return CommentUpdate(content = content)
    }
}
