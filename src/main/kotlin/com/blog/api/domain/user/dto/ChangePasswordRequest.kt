package com.blog.api.domain.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "현재 비밀번호는 필수입니다")
    val currentPassword: String,

    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]+\$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    val newPassword: String
)
