package com.blog.api.core.domain

import com.blog.api.storage.db.core.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    fun getAllCategories(): List<Category> {
        return categoryRepository.findAll()
            .map {
                Category(
                    id = it.id!!,
                    name = it.name,
                    slug = it.slug,
                    createdAt = it.createdAt
                )
            }
    }
}
