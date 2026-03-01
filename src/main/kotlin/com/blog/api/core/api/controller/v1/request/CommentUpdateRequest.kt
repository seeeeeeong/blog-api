package com.blog.api.core.api.controller.v1.request

import com.blog.api.core.domain.CommentUpdate
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CommentUpdateRequest(
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String
) {
    fun toCommentUpdate(): CommentUpdate {
        return CommentUpdate(content = content)
    }
}
