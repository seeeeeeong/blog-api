package com.blog.api.core.api.controller.v1

import com.blog.api.core.api.controller.v1.request.CommentCreateRequest
import com.blog.api.core.api.controller.v1.request.CommentUpdateRequest
import com.blog.api.core.api.controller.v1.response.CommentResponse
import com.blog.api.core.support.web.annotation.GitHubAuth
import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.integration.oauth.GitHubOAuthUser
import com.blog.api.core.domain.CommentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
class CommentController(
    private val commentService: CommentService
) {

    @Operation(summary = "댓글 작성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createComment(
        @PathVariable postId: Long,
        @GitHubAuth githubUser: GitHubOAuthUser,
        @RequestBody request: CommentCreateRequest
    ): ApiResponse<CommentResponse> {
        val comment = commentService.createComment(request.toCommentCreate(postId, githubUser))
        return ApiResponse.success(CommentResponse.of(comment))
    }

    @Operation(summary = "댓글 목록 조회")
    @GetMapping
    fun getComments(
        @PathVariable postId: Long
    ): ApiResponse<List<CommentResponse>> {
        val comments = commentService.getCommentsByPost(postId)
        return ApiResponse.success(CommentResponse.of(comments))
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @GitHubAuth githubUser: GitHubOAuthUser,
        @RequestBody request: CommentUpdateRequest
    ): ApiResponse<CommentResponse> {
        val comment = commentService.updateComment(commentId, githubUser.id.toString(), request.toCommentUpdate())
        return ApiResponse.success(CommentResponse.of(comment))
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @GitHubAuth githubUser: GitHubOAuthUser
    ): ApiResponse<Any> {
        commentService.deleteComment(commentId, githubUser.id.toString())
        return ApiResponse.success()
    }
}
