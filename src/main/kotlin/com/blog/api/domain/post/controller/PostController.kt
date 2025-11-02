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
        val post = postService.createPost(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(post)
    }
    
    @GetMapping("/{postId}")
    fun getPost(@PathVariable postId: Long): ResponseEntity<PostResponse> {
        val post = postService.getPost(postId)
        return ResponseEntity.ok(post)
    }
    
    @GetMapping
    fun getAllPosts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PostListResponse> {
        val posts = postService.getAllPosts(page, size)
        return ResponseEntity.ok(posts)
    }
    
    @GetMapping("/category/{categoryId}")
    fun getPostsByCategory(
        @PathVariable categoryId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PostListResponse> {
        val posts = postService.getPostsByCategory(categoryId, page, size)
        return ResponseEntity.ok(posts)
    }
    
    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @RequestHeader("User-Id") userId: Long,
        @Valid @RequestBody request: UpdatePostRequest
    ): ResponseEntity<PostResponse> {
        val post = postService.updatePost(postId, userId, request)
        return ResponseEntity.ok(post)
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
