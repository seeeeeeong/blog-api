package com.blog.api.core.api.controller.v1.request

import jakarta.validation.constraints.NotBlank

data class CommentDeleteRequest(
    @field:NotBlank val password: String,
)
