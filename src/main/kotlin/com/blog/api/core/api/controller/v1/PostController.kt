package com.blog.api.core.api.controller.v1

import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.support.response.PageResponse
import com.blog.api.core.support.auth.Admin
import com.blog.api.core.api.controller.v1.reqeust.PostCreateRequest
import jakarta.servlet.http.HttpServletRequest
import com.blog.api.core.api.controller.v1.reqeust.PostUpdateRequest
import com.blog.api.core.api.controller.v1.response.PostResponse
import com.blog.api.core.domain.PostService
import com.blog.api.core.support.web.HttpServletRequestUtils
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val postService: PostService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @Admin userId: Long,
        @RequestBody request: PostCreateRequest
    ): ApiResponse<PostResponse> {
        val post = postService.createPost(request.toPostCreate(userId))
        return ApiResponse.success(PostResponse.of(post))
    }

    @GetMapping("/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        request: HttpServletRequest,
    ): ApiResponse<PostResponse> {
        val clientIp = HttpServletRequestUtils.resolveClientIp(request)
        val post = postService.getPost(postId, clientIp)
        return ApiResponse.success(PostResponse.of(post))
    }

    @GetMapping
    fun getAllPosts(
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostResponse>> {
        val posts = postService.getAllPosts(pageable)
        return ApiResponse.success(
            PageResponse(
                posts.content.map(PostResponse.Companion::of),
                posts.hasNext()
            )
        )
    }

    @GetMapping("/categories/{categoryId}")
    fun getPostsByCategory(
        @PathVariable categoryId: Long,
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostResponse>> {
        val posts = postService.getPostsByCategory(categoryId, pageable)
        return ApiResponse.success(
            PageResponse(
                posts.content.map(PostResponse.Companion::of),
                posts.hasNext()
            )
        )
    }

    @GetMapping("/drafts")
    fun getDraftPosts(
        @Admin userId: Long,
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostResponse>> {
        val posts = postService.getDraftPosts(userId, pageable)
        return ApiResponse.success(
            PageResponse(
                posts.content.map(PostResponse.Companion::of),
                posts.hasNext()
            )
        )
    }

    @GetMapping("/search")
    fun searchPosts(
        @RequestParam query: String,
        @RequestParam(required = false) categoryId: Long?,
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostResponse>> {
        val posts = postService.searchPosts(query, categoryId, pageable)
        return ApiResponse.success(
            PageResponse(
                posts.content.map(PostResponse.Companion::of),
                posts.hasNext()
            )
        )
    }

    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @Admin userId: Long,
        @RequestBody request: PostUpdateRequest
    ): ApiResponse<PostResponse> {
        val post = postService.updatePost(postId, userId, request.toPostUpdate())
        return ApiResponse.success(PostResponse.of(post))
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(
        @PathVariable postId: Long,
        @Admin userId: Long
    ): ApiResponse<Any> {
        postService.deletePost(postId, userId)
        return ApiResponse.success()
    }
}
