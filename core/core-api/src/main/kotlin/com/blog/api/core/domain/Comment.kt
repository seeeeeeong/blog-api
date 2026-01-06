package com.blog.api.core.domain

import java.time.LocalDateTime

data class Comment(
    val id: Long,
    val postId: Long,
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String,
    val parentId: Long,
    val content: String,
    val contentHtml: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
