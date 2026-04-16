package com.blog.api.core.api.controller.v1

import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.support.response.PageResponse
import com.blog.api.core.support.auth.Admin
import com.blog.api.core.support.auth.CurrentUser
import com.blog.api.core.support.auth.ResolveCurrentUser
import com.blog.api.core.api.controller.v1.request.PostCreateRequest
import com.blog.api.core.api.controller.v1.request.PostUpdateRequest
import com.blog.api.core.api.controller.v1.response.PostResponse
import com.blog.api.core.api.controller.v1.response.CommentResponse
import com.blog.api.core.api.controller.v1.response.PostSummaryResponse
import com.blog.api.core.domain.comment.CommentService
import com.blog.api.core.domain.post.PostService
import com.blog.api.core.enum.UserRole
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
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

@Validated
@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val postService: PostService,
    private val commentService: CommentService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @Admin userId: Long,
        @Valid @RequestBody request: PostCreateRequest
    ): ApiResponse<PostResponse> {
        val post = postService.createPost(request.toCommand(userId))
        return ApiResponse.success(PostResponse.of(post, postService.renderHtml(post.id, post.content)))
    }

    @GetMapping("/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        @ResolveCurrentUser currentUser: CurrentUser,
    ): ApiResponse<PostResponse> {
        val post = postService.getPost(postId, currentUser.userId, currentUser.role == UserRole.ADMIN)
        val comments = commentService.getCommentsByPost(postId).map(CommentResponse::of)
        return ApiResponse.success(PostResponse.of(post, postService.renderHtml(post.id, post.content), comments))
    }

    @GetMapping("/{postId}/admin")
    fun getPostForAdmin(
        @PathVariable postId: Long,
        @Admin userId: Long,
    ): ApiResponse<PostResponse> {
        val post = postService.getPostForAdmin(postId, userId)
        val comments = commentService.getCommentsByPost(postId).map(CommentResponse::of)
        return ApiResponse.success(PostResponse.of(post, postService.renderHtml(post.id, post.content), comments))
    }

    @GetMapping
    fun getAllPosts(
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostSummaryResponse>> {
        val posts = postService.getAllPosts(pageable)
        return ApiResponse.success(
            PageResponse(
                posts.content.map(PostSummaryResponse.Companion::of),
                posts.hasNext()
            )
        )
    }

    @GetMapping("/categories/{categoryId}")
    fun getPostsByCategory(
        @PathVariable categoryId: Long,
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostSummaryResponse>> {
        val posts = postService.getPostsByCategory(categoryId, pageable)
        return ApiResponse.success(
            PageResponse(
                posts.content.map(PostSummaryResponse.Companion::of),
                posts.hasNext()
            )
        )
    }

    @GetMapping("/drafts")
    fun getDraftPosts(
        @Admin userId: Long,
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostSummaryResponse>> {
        val posts = postService.getDraftPosts(userId, pageable)
        return ApiResponse.success(
            PageResponse(
                posts.content.map(PostSummaryResponse.Companion::of),
                posts.hasNext()
            )
        )
    }

    @GetMapping("/search")
    fun searchPosts(
        @RequestParam query: String,
        @RequestParam(required = false) categoryId: Long?,
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostSummaryResponse>> {
        val posts = postService.searchPosts(query, categoryId, pageable)
        return ApiResponse.success(
            PageResponse(
                posts.content.map(PostSummaryResponse.Companion::of),
                posts.hasNext()
            )
        )
    }

    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @Admin userId: Long,
        @Valid @RequestBody request: PostUpdateRequest
    ): ApiResponse<PostResponse> {
        val post = postService.updatePost(postId, userId, request.toCommand())
        return ApiResponse.success(PostResponse.of(post, postService.renderHtml(post.id, post.content)))
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
