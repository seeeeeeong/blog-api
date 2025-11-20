package com.blog.api.domain.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreatePostRequest(

    @field:NotNull(message = "카테고리는 필수입니다")
    val categoryId: Long,

    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    val content: String,

    val thumbnailUrl: String? = null,

    val isDraft: Boolean = false
)

data class UpdatePostRequest(

    @field:NotNull(message = "카테고리는 필수입니다")
    val categoryId: Long,

    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    val content: String,

    val thumbnailUrl: String? = null,

    val isDraft: Boolean = false
)