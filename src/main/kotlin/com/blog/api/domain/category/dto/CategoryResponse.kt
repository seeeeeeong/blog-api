package com.blog.api.domain.category.dto

import com.blog.api.domain.category.entity.Category
import java.time.LocalDateTime

data class CategoryResponse(
    val id: Long,
    val name: String,
    val slug: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(category: Category): CategoryResponse {
            return CategoryResponse(
                id = category.id!!,
                name = category.name,
                slug = category.slug,
                createdAt = category.createdAt
            )
        }
    }
}
