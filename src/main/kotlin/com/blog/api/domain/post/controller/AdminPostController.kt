package com.blog.api.domain.post.controller

import com.blog.api.domain.post.service.AdminPostService
import com.blog.api.global.response.ApiResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/posts")
class AdminPostController(
    private val adminPostService: AdminPostService
) {

    @GetMapping
    fun getAllPosts(
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ) = ApiResponse.success(
        adminPostService.getAllPosts(pageable)
    )

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(@PathVariable postId: Long): ApiResponse<Unit> {
        adminPostService.deletePost(postId)
        return ApiResponse.success(Unit)
    }
}