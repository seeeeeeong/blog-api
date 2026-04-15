package com.blog.api.storage.category

import com.blog.api.core.domain.category.Category

fun CategoryEntity.toCategory() = Category(
    id = requireNotNull(id) { "CategoryEntity.id must not be null after persistence" },
    name = name,
    slug = slug,
    createdAt = createdAt,
)
