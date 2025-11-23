package com.blog.api.domain.post.controller

import com.blog.api.domain.post.service.AdminPostService
import com.blog.api.global.response.ApiResponse
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/posts")
class AdminPostController(
    private val adminPostService: AdminPostService
) {

    @GetMapping
    fun getAllPosts(pageable: Pageable) =
        ApiResponse.success(adminPostService.getAllPosts(pageable))

    @DeleteMapping("/{postId}")
    fun deletePost(@PathVariable postId: Long) =
        adminPostService.deletePost(postId).let { ApiResponse.success(Unit) }
}
