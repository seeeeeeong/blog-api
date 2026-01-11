package com.blog.api.core.api.controller.v1.response

import com.blog.api.core.domain.Category
import java.time.LocalDateTime

data class CategoryResponse(
    val id: Long,
    val name: String,
    val slug: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun of(category: Category): CategoryResponse {
            return CategoryResponse(
                id = category.id,
                name = category.name,
                slug = category.slug,
                createdAt = category.createdAt
            )
        }
    }
}