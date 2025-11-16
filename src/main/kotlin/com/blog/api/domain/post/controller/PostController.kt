package com.blog.api.domain.post.controller

import com.blog.api.domain.post.dto.CreatePostRequest
import com.blog.api.domain.post.dto.PostListResponse
import com.blog.api.domain.post.dto.PostResponse
import com.blog.api.domain.post.dto.UpdatePostRequest
import com.blog.api.domain.post.service.PostService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {
    
    @PostMapping
    fun createPost(
        @RequestHeader("User-Id") userId: Long,
        @Valid @RequestBody request: CreatePostRequest
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body( postService.createPost(userId, request))
    }
    
    @GetMapping("/{postId}")
    fun getPost(@PathVariable postId: Long): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postService.getPost(postId))
    }
    
    @GetMapping
    fun getAllPosts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PostListResponse> {
        return ResponseEntity.ok(postService.getAllPosts(page, size))
    }
    
    @GetMapping("/category/{categoryId}")
    fun getPostsByCategory(
        @PathVariable categoryId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PostListResponse> {
        return ResponseEntity.ok(postService.getPostsByCategory(categoryId, page, size))
    }
    
    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @RequestHeader("User-Id") userId: Long,
        @Valid @RequestBody request: UpdatePostRequest
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postService.updatePost(postId, userId, request))
    }
    
    @DeleteMapping("/{postId}")
    fun deletePost(
        @PathVariable postId: Long,
        @RequestHeader("User-Id") userId: Long
    ): ResponseEntity<Void> {
        postService.deletePost(postId, userId)
        return ResponseEntity.noContent().build()
    }
}
