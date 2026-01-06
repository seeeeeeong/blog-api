package com.blog.api.core.api.controller.v1

import com.blog.api.core.api.controller.v1.response.CategoryResponse
import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.domain.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Category", description = "카테고리 API")
@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @Operation(summary = "카테고리 목록 조회")
    @GetMapping
    fun getAllCategories(): ApiResponse<List<CategoryResponse>> {
        val categories = categoryService.getAllCategories()
        return ApiResponse.success(CategoryResponse.of(categories))
    }

}
