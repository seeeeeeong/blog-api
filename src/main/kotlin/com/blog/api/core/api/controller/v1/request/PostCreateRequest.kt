package com.blog.api.core.api.controller.v1.request

import com.blog.api.core.domain.post.PostCreate
import com.blog.api.core.enum.PostStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PostCreateRequest(
    val categoryId: Long,
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:NotBlank
    @field:Size(max = 50000)
    val content: String,
    val thumbnailUrl: String? = null,
    val isDraft: Boolean = false
) {
    fun toCommand(userId: Long): PostCreate {
        return PostCreate(
            userId = userId,
            categoryId = categoryId,
            title = title,
            content = content,
            thumbnailUrl = thumbnailUrl,
            status = if (isDraft) PostStatus.DRAFT else PostStatus.PUBLISHED
        )
    }
}
