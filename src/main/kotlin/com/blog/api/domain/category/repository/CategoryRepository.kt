package com.blog.api.domain.category.repository

import com.blog.api.domain.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {
    
    fun findBySlug(slug: String): Category?
}
