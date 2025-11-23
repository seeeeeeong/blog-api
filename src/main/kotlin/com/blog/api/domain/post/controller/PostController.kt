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
    ) = ApiResponse.success(
        postService.createPost(userId, request)
    )

    @GetMapping("/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        @ClientIp clientIp: String
    ) = ApiResponse.success(
        postService.getPost(postId, clientIp)
    )

    @GetMapping
    fun getAllPosts(
        pageable: Pageable
    ) = ApiResponse.success(
        postService.getAllPosts(pageable)
    )

    @GetMapping("/categories/{categoryId}")
    fun getPostsByCategory(
        @PathVariable categoryId: Long,
        pageable: Pageable
    ) = ApiResponse.success(
        postService.getPostsByCategory(categoryId, pageable)
    )

    @GetMapping("/users/me/posts")
    fun getMyPosts(
        @AuthUser userId: Long,
        pageable: Pageable
    ) = ApiResponse.success(
        postService.getMyPosts(userId, pageable)
    )

    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @AuthUser userId: Long,
        @Valid @RequestBody request: UpdatePostRequest
    ) = ApiResponse.success(
        postService.updatePost(postId, userId, request)
    )

    @DeleteMapping("/{postId}")
    fun deletePost(
        @PathVariable postId: Long,
        @AuthUser userId: Long
    ) = postService.deletePost(postId, userId).let {
        ApiResponse.success(Unit)
    }
}
