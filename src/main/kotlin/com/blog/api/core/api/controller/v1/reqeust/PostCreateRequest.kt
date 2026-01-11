package com.blog.api.core.api.controller.v1.reqeust

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.domain.PostCreate
import com.blog.api.core.enum.PostStatus

data class PostCreateRequest(
    val categoryId: Long,
    val title: String,
    val content: String,
    val thumbnailUrl: String? = null,
    val isDraft: Boolean = false
) {
    fun toPostCreate(userId: Long): PostCreate {
        if (title.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)
        if (title.length > 200) throw CoreException(ErrorType.INVALID_INPUT)
        if (content.isBlank()) throw CoreException(ErrorType.INVALID_INPUT)

        return PostCreate(
            userId = userId,
            categoryId = categoryId,
            title = title,
            content = content,
            thumbnailUrl = thumbnailUrl ?: "",
            status = if (isDraft) PostStatus.DRAFT else PostStatus.PUBLISHED
        )
    }
}
