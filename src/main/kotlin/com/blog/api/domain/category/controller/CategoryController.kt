package com.blog.api.domain.category.controller

import com.blog.api.domain.category.service.CategoryService
import com.blog.api.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Category", description = "카테고리 API")
@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @Operation(
        summary = "카테고리 목록 조회",
        description = "모든 카테고리 목록을 조회합니다."
    )
    @GetMapping
    fun getAllCategories() = ApiResponse.success(
        categoryService.getAllCategories()
    )
}