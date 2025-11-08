package com.blog.api.domain.category.controller

import com.blog.api.domain.category.dto.CategoryResponse
import com.blog.api.domain.category.service.CategoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {
    
    @GetMapping
    fun getAllCategories(): ResponseEntity<List<CategoryResponse>> {
        return ResponseEntity.ok(categoryService.getAllCategories())
    }
}
