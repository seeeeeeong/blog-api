package com.blog.api.domain.post.controller

import com.blog.api.domain.post.dto.CreatePostRequest
import com.blog.api.domain.post.dto.UpdatePostRequest
import com.blog.api.domain.post.service.PostService
import com.blog.api.common.web.annotation.AuthUser
import com.blog.api.common.web.annotation.ClientIp
import com.blog.api.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Post", description = "블로그 게시글 API")
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {

    @Operation(
        summary = "게시글 작성",
        description = "새로운 블로그 게시글을 작성합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Valid @RequestBody request: CreatePostRequest
    ) = ApiResponse.success(
        postService.createPost(userId, request)
    )

    @Operation(
        summary = "게시글 상세 조회",
        description = "게시글 ID로 상세 정보를 조회합니다. 조회 시 조회수가 증가합니다."
    )
    @GetMapping("/{postId}")
    fun getPost(
        @Parameter(description = "게시글 ID") @PathVariable postId: Long,
        @Parameter(hidden = true) @ClientIp clientIp: String
    ) = ApiResponse.success(
        postService.getPost(postId, clientIp)
    )

    @Operation(
        summary = "게시글 목록 조회",
        description = "발행된 모든 게시글을 페이징하여 조회합니다."
    )
    @GetMapping
    fun getAllPosts(
        @Parameter(description = "페이징 정보 (page, size)")
        @PageableDefault(size = 10)
        pageable: Pageable
    ) = ApiResponse.success(
        postService.getAllPosts(pageable)
    )

    @Operation(
        summary = "카테고리별 게시글 조회",
        description = "특정 카테고리의 게시글을 페이징하여 조회합니다."
    )
    @GetMapping("/categories/{categoryId}")
    fun getPostsByCategory(
        @Parameter(description = "카테고리 ID") @PathVariable categoryId: Long,
        @Parameter(description = "페이징 정보 (page, size)")
        @PageableDefault(size = 10)
        pageable: Pageable
    ) = ApiResponse.success(
        postService.getPostsByCategory(categoryId, pageable)
    )

    @Operation(
        summary = "게시글 검색",
        description = "제목 또는 내용으로 게시글을 검색합니다. 키워드와 카테고리로 필터링할 수 있습니다."
    )
    @GetMapping("/search")
    fun searchPosts(
        @Parameter(description = "검색 키워드 (제목 또는 내용)") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "카테고리 ID") @RequestParam(required = false) categoryId: Long?,
        @Parameter(description = "페이징 정보 (page, size)")
        @PageableDefault(size = 10)
        pageable: Pageable
    ) = ApiResponse.success(
        postService.searchPosts(keyword, categoryId, pageable)
    )

    @Operation(
        summary = "내 게시글 조회",
        description = "로그인한 사용자의 모든 게시글(발행+임시저장)을 페이징하여 조회합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/my")
    fun getMyPosts(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Parameter(description = "페이징 정보 (page, size)")
        @PageableDefault(size = 10)
        pageable: Pageable
    ) = ApiResponse.success(
        postService.getMyPosts(userId, pageable)
    )

    @Operation(
        summary = "임시저장 게시글 조회",
        description = "로그인한 사용자의 임시저장된 게시글을 페이징하여 조회합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/drafts")
    fun getDraftPosts(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Parameter(description = "페이징 정보 (page, size)")
        @PageableDefault(size = 10)
        pageable: Pageable
    ) = ApiResponse.success(
        postService.getDraftPosts(userId, pageable)
    )

    @Operation(
        summary = "인기 게시글 조회",
        description = "조회수가 높은 상위 N개의 게시글을 조회합니다. (기본: 10개)"
    )
    @GetMapping("/popular")
    fun getPopularPosts(
        @Parameter(description = "조회할 게시글 수 (기본: 10)") @RequestParam(defaultValue = "10") limit: Int
    ) = ApiResponse.success(
        postService.getPopularPosts(limit)
    )

    @Operation(
        summary = "게시글 수정",
        description = "게시글 ID로 게시글을 수정합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PutMapping("/{postId}")
    fun updatePost(
        @Parameter(description = "게시글 ID") @PathVariable postId: Long,
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Valid @RequestBody request: UpdatePostRequest
    ) = ApiResponse.success(
        postService.updatePost(postId, userId, request)
    )

    @Operation(
        summary = "게시글 삭제",
        description = "게시글 ID로 게시글을 삭제합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/{postId}")
    fun deletePost(
        @Parameter(description = "게시글 ID") @PathVariable postId: Long,
        @Parameter(hidden = true) @AuthUser userId: Long
    ): ApiResponse<Unit> {
        postService.deletePost(postId, userId)
        return ApiResponse.success(Unit)
    }
}