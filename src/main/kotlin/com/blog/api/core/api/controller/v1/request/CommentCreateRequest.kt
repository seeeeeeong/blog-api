package com.blog.api.core.api.controller.v1.request

import com.blog.api.core.domain.comment.CommentCreate
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CommentCreateRequest(
    @field:NotBlank @field:Size(max = 20) val nickname: String,
    @field:NotBlank @field:Size(max = 50) val password: String,
    @field:NotBlank @field:Size(max = 1000) val content: String,
    val parentId: Long? = null,
) {
    fun toCommand(postId: Long) = CommentCreate(
        postId = postId,
        nickname = nickname,
        password = password,
        parentId = parentId,
        content = content,
    )
}
