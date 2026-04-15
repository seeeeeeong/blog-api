package com.blog.api.core.api.controller.v1.request

import com.blog.api.core.domain.comment.CommentUpdate
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CommentUpdateRequest(
    @field:NotBlank @field:Size(max = 50) val password: String,
    @field:NotBlank @field:Size(max = 1000) val content: String,
) {
    fun toCommand() = CommentUpdate(password = password, content = content)
}
