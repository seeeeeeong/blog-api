package com.blog.api.core.api.controller.v1

import com.blog.api.core.api.controller.v1.response.CategoryResponse
import com.blog.api.core.domain.category.CategoryService
import com.blog.api.core.support.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val categoryService: CategoryService,
) {
    @GetMapping
    fun getAllCategories(): ApiResponse<List<CategoryResponse>> {
        val categories = categoryService.getAllCategories()
        return ApiResponse.success(categories.map { CategoryResponse.of(it) })
    }
}
