package com.blog.api.core.domain.category

import com.blog.api.core.api.config.CacheConfig
import com.blog.api.storage.category.CategoryRepository
import com.blog.api.storage.category.toCategory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {

    @Cacheable(CacheConfig.CATEGORIES, sync = true)
    fun getAllCategories(): List<Category> =
        categoryRepository.findAll().map { it.toCategory() }
}
