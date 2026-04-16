package com.blog.api.core.api.controller.v1

import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.support.response.PageResponse
import com.blog.api.core.support.auth.Admin
import com.blog.api.core.api.controller.v1.request.PostCreateRequest
import com.blog.api.core.api.controller.v1.request.PostUpdateRequest
import com.blog.api.core.api.controller.v1.response.PostResponse
import com.blog.api.core.api.controller.v1.response.CommentResponse
import com.blog.api.core.api.controller.v1.response.PostSummaryResponse
import com.blog.api.core.domain.comment.CommentService
import com.blog.api.core.domain.post.PostService
import com.blog.api.core.domain.post.PostViewCommand
import com.blog.api.core.enum.PostStatus
import com.blog.api.core.enum.UserRole
import com.blog.api.core.support.web.HttpServletRequestUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.security.core.context.SecurityContextHolder
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

    companion object {
        private const val VIEW_COOKIE_MAX_AGE = 3600L
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @Admin userId: Long,
        @Valid @RequestBody request: PostCreateRequest
    ): ApiResponse<PostResponse> {
        val post = postService.createPost(request.toPostCreate(userId))
        return ApiResponse.success(PostResponse.of(post, postService.getHtml(post.id, post.content)))
    }

    @GetMapping("/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<PostResponse> {
        val command = buildPostViewCommand(postId, request)
        val post = postService.getPost(command)
        if (!command.hasViewedCookie && post.status == PostStatus.PUBLISHED) {
            addViewedCookie(response, postId)
        }
        val comments = commentService.getCommentsByPost(postId).map(CommentResponse::of)
        return ApiResponse.success(PostResponse.of(post, postService.getHtml(post.id, post.content), comments))
    }

    @GetMapping("/{postId}/admin")
    fun getPostForAdmin(
        @PathVariable postId: Long,
        @Admin userId: Long,
    ): ApiResponse<PostResponse> {
        val post = postService.getPostForAdmin(postId, userId)
        val comments = commentService.getCommentsByPost(postId).map(CommentResponse::of)
        return ApiResponse.success(PostResponse.of(post, postService.getHtml(post.id, post.content), comments))
    }

    @GetMapping("/popular")
    fun getPopularPosts(
        @RequestParam(defaultValue = "5") @Min(1) @Max(20) limit: Int,
    ): ApiResponse<List<PostSummaryResponse>> {
        val posts = postService.getPopularPosts(limit)
        return ApiResponse.success(posts.map { PostSummaryResponse.of(it) })
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
        val post = postService.updatePost(postId, userId, request.toPostUpdate())
        return ApiResponse.success(PostResponse.of(post, postService.getHtml(post.id, post.content)))
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

    private fun buildPostViewCommand(
        postId: Long,
        request: HttpServletRequest,
    ): PostViewCommand {
        val authentication = SecurityContextHolder.getContext().authentication
        val cookieName = "viewed_post_$postId"
        return PostViewCommand(
            postId = postId,
            hasViewedCookie = HttpServletRequestUtils.getCookieValue(request, cookieName) != null,
            viewerUserId = authentication?.principal as? Long,
            viewerRole = extractUserRole(authentication),
        )
    }

    private fun addViewedCookie(response: HttpServletResponse, postId: Long) {
        val cookie = ResponseCookie.from("viewed_post_$postId", "1")
            .httpOnly(true)
            .path("/")
            .maxAge(VIEW_COOKIE_MAX_AGE)
            .sameSite("None")
            .secure(true)
            .build()
        response.addHeader("Set-Cookie", cookie.toString())
    }

    private fun extractUserRole(authentication: org.springframework.security.core.Authentication?): UserRole? {
        if (authentication == null) return null
        val hasAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" }
        return if (hasAdmin) UserRole.ADMIN else UserRole.USER
    }
}
