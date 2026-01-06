package com.blog.api.core.api.controller.v1

import com.blog.api.core.api.controller.v1.request.PostCreateRequest
import com.blog.api.core.api.controller.v1.request.PostUpdateRequest
import com.blog.api.core.api.controller.v1.response.PostResponse
import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.support.response.PageResponse
import com.blog.api.core.support.web.annotation.AuthUser
import com.blog.api.core.support.web.annotation.ClientIp
import com.blog.api.core.domain.PostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Post", description = "블로그 게시글 API")
@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val postService: PostService
) {

    @Operation(summary = "게시글 작성", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @RequestBody request: PostCreateRequest
    ): ApiResponse<PostResponse> {
        val post = postService.createPost(request.toPostCreate(userId))
        return ApiResponse.success(PostResponse.of(post))
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        @Parameter(hidden = true) @ClientIp clientIp: String
    ): ApiResponse<PostResponse> {
        val post = postService.getPost(postId, clientIp)
        return ApiResponse.success(PostResponse.of(post))
    }

    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    fun getAllPosts(
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostResponse>> {
        val posts = postService.getAllPosts(pageable)
        return ApiResponse.success(PageResponse(PostResponse.of(posts.content), posts.hasNext()))
    }

    @Operation(summary = "카테고리별 게시글 조회")
    @GetMapping("/categories/{categoryId}")
    fun getPostsByCategory(
        @PathVariable categoryId: Long,
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostResponse>> {
        val posts = postService.getPostsByCategory(categoryId, pageable)
        return ApiResponse.success(PageResponse(PostResponse.of(posts.content), posts.hasNext()))
    }

    @Operation(summary = "내 게시글 조회", security = [SecurityRequirement(name = "bearerAuth")])
    @GetMapping("/my")
    fun getMyPosts(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostResponse>> {
        val posts = postService.getMyPosts(userId, pageable)
        return ApiResponse.success(PageResponse(PostResponse.of(posts.content), posts.hasNext()))
    }

    @Operation(summary = "임시저장 게시글 조회", security = [SecurityRequirement(name = "bearerAuth")])
    @GetMapping("/drafts")
    fun getDraftPosts(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostResponse>> {
        val posts = postService.getDraftPosts(userId, pageable)
        return ApiResponse.success(PageResponse(PostResponse.of(posts.content), posts.hasNext()))
    }

    @Operation(summary = "인기 게시글 조회")
    @GetMapping("/popular")
    fun getPopularPosts(
        @RequestParam(defaultValue = "10") limit: Int
    ): ApiResponse<List<PostResponse>> {
        val posts = postService.getPopularPosts(limit)
        return ApiResponse.success(PostResponse.of(posts))
    }

    @Operation(summary = "유사도 기반 게시글 검색 (AI)")
    @GetMapping("/search/similarity")
    fun searchPostsBySimilarity(
        @RequestParam query: String,
        @RequestParam(required = false) categoryId: Long?,
        @PageableDefault(size = 10) pageable: Pageable
    ): ApiResponse<PageResponse<PostResponse>> {
        val posts = postService.searchPostsBySimilarity(query, categoryId, pageable)
        return ApiResponse.success(PageResponse(PostResponse.of(posts.content), posts.hasNext()))
    }

    @Operation(summary = "관련 게시글 추천")
    @GetMapping("/{postId}/similar")
    fun getSimilarPosts(
        @PathVariable postId: Long,
        @RequestParam(defaultValue = "5") limit: Int
    ): ApiResponse<List<PostResponse>> {
        val posts = postService.getSimilarPosts(postId, limit)
        return ApiResponse.success(PostResponse.of(posts))
    }

    @Operation(summary = "게시글 수정", security = [SecurityRequirement(name = "bearerAuth")])
    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @Parameter(hidden = true) @AuthUser userId: Long,
        @RequestBody request: PostUpdateRequest
    ): ApiResponse<PostResponse> {
        val post = postService.updatePost(postId, userId, request.toPostUpdate())
        return ApiResponse.success(PostResponse.of(post))
    }

    @Operation(summary = "게시글 삭제", security = [SecurityRequirement(name = "bearerAuth")])
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(
        @PathVariable postId: Long,
        @Parameter(hidden = true) @AuthUser userId: Long
    ): ApiResponse<Any> {
        postService.deletePost(postId, userId)
        return ApiResponse.success()
    }
}
