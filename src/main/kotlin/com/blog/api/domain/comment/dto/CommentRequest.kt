package com.blog.api.domain.comment.dto

import jakarta.validation.constraints.NotBlank

data class CreateCommentRequest(
    
    @field:NotBlank(message = "댓글 내용은 필수입니다")
    val content: String,
    
    val parentId: Long? = null
)

data class UpdateCommentRequest(
    
    @field:NotBlank(message = "댓글 내용은 필수입니다")
    val content: String
)
