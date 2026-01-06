package com.blog.api.core.domain

import java.time.LocalDateTime

data class Category(
    val id: Long,
    val name: String,
    val slug: String,
    val createdAt: LocalDateTime
)
