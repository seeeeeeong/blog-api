package com.blog.api.domain.post.controller

import com.blog.api.domain.post.dto.CreatePostRequest
import com.blog.api.domain.post.dto.PostListResponse
import com.blog.api.domain.post.dto.PostResponse
import com.blog.api.domain.post.dto.UpdatePostRequest
import com.blog.api.domain.post.service.PostService
import com.blog.api.global.auth.AuthUser
import com.blog.api.global.auth.ClientIp
import com.blog.api.global.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {

    @PostMapping
    fun createPost(
        @AuthUser userId: Long,
        @Valid @RequestBody request: CreatePostRequest
    ): ApiResponse<PostResponse> {
        return ApiResponse.success(postService.createPost(userId, request))
    }

    @GetMapping("/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        @ClientIp clientIp: String
    ): ApiResponse<PostResponse> {
        return ApiResponse.success(postService.getPost(postId, clientIp))
    }

    @GetMapping
    fun getAllPosts(
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): ApiResponse<PostListResponse> {
        return ApiResponse.success(postService.getAllPosts(pageable))
    }

    @GetMapping("/category/{categoryId}")
    fun getPostsByCategory(
        @PathVariable categoryId: Long,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): ApiResponse<PostListResponse> {
        return ApiResponse.success(postService.getPostsByCategory(categoryId, pageable))
    }

    @GetMapping("/my")
    fun getMyPosts(
        @AuthUser userId: Long,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): ApiResponse<PostListResponse> {
        return ApiResponse.success(postService.getMyPosts(userId, pageable))
    }

    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @AuthUser userId: Long,
        @Valid @RequestBody request: UpdatePostRequest
    ): ApiResponse<PostResponse> {
        return ApiResponse.success(postService.updatePost(postId, userId, request))
    }

    @DeleteMapping("/{postId}")
    fun deletePost(
        @PathVariable postId: Long,
        @AuthUser userId: Long
    ): ApiResponse<Unit> {
        postService.deletePost(postId, userId)
        return ApiResponse.success()
    }
}