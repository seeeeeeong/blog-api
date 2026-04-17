package com.blog.api.core.domain.comment

import java.time.LocalDateTime

data class Comment(
    val id: Long,
    val postId: Long,
    val nickname: String,
    val content: String,
    val contentHtml: String,
    val createdAt: LocalDateTime,
)
