package com.blog.api.core.api.controller.v1.request

import com.blog.api.core.domain.comment.CommentCreate
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CommentCreateRequest(
    @field:NotBlank @field:Size(max = 1000) val content: String,
) {
    fun toCommand(postId: Long) = CommentCreate(
        postId = postId,
        content = content,
    )
}
