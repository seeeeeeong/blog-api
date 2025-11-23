package com.blog.api.domain.comment.controller

import com.blog.api.domain.comment.dto.CreateCommentRequest
import com.blog.api.domain.comment.dto.UpdateCommentRequest
import com.blog.api.domain.comment.service.CommentService
import com.blog.api.global.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts/{postId}/comments")
class CommentController(
    private val commentService: CommentService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createComment(
        @PathVariable postId: Long,
        @RequestHeader("Authorization") authorization: String,
        @RequestHeader("GitHub-Username") githubUsername: String,
        @RequestHeader("GitHub-Avatar-Url", required = false) githubAvatarUrl: String?,
        @Valid @RequestBody request: CreateCommentRequest
    ) = ApiResponse.success(
        commentService.createComment(
            postId,
            extractToken(authorization),
            githubUsername,
            githubAvatarUrl,
            request
        )
    )

    @GetMapping
    fun getComments(
        @PathVariable postId: Long
    ) = ApiResponse.success(
        commentService.getCommentsByPost(postId)
    )

    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable postId: Long, // URL 경로상 필요하지만 사용하지 않는다면 생략하거나 포함
        @PathVariable commentId: Long,
        @RequestHeader("Authorization") authorization: String,
        @Valid @RequestBody request: UpdateCommentRequest
    ) = ApiResponse.success(
        commentService.updateComment(commentId, extractToken(authorization), request)
    )

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @RequestHeader("Authorization") authorization: String
    ): ApiResponse<Unit> {
        commentService.deleteComment(commentId, extractToken(authorization))
        return ApiResponse.success(Unit)
    }

    private fun extractToken(authorization: String): String {
        return authorization.removePrefix("Bearer ")
    }
}