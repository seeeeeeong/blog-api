package com.blog.api.domain.category.service

import com.blog.api.domain.category.dto.CategoryResponse
import com.blog.api.domain.category.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository
) {
    
    fun getAllCategories(): List<CategoryResponse> {
        return categoryRepository.findAll()
            .map { CategoryResponse.from(it) }
    }
}
