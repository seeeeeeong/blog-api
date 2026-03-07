package com.blog.api.core.domain

import com.blog.api.storage.CategoryRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {

    @Cacheable("categories")
    fun getAllCategories(): List<Category> {
        return categoryRepository.findAll()
            .map { category ->
                Category(
                    id = category.id!!,
                    name = category.name,
                    slug = category.slug,
                    createdAt = category.createdAt,
                )
            }
    }
}
