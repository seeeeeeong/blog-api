package com.blog.api.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(min = 2, max = 50, message = "닉네임은 2-50자 사이여야 합니다")
    val nickname: String,

    val profileImageUrl: String? = null
)
