package com.blog.api.domain.comment.controller

import com.blog.api.domain.comment.dto.CreateCommentRequest
import com.blog.api.domain.comment.dto.UpdateCommentRequest
import com.blog.api.domain.comment.service.CommentService
import com.blog.api.common.web.annotation.GitHubAuth
import com.blog.api.common.web.dto.GitHubUser
import com.blog.api.common.response.ApiResponse
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
        @GitHubAuth githubUser: GitHubUser,
        @Valid @RequestBody request: CreateCommentRequest
    ) = ApiResponse.success(
        commentService.createComment(postId, githubUser, request)
    )

    @GetMapping
    fun getComments(
        @PathVariable postId: Long
    ) = ApiResponse.success(
        commentService.getCommentsByPost(postId)
    )

    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @GitHubAuth githubUser: GitHubUser,
        @Valid @RequestBody request: UpdateCommentRequest
    ) = ApiResponse.success(
        commentService.updateComment(commentId, githubUser, request)
    )

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @GitHubAuth githubUser: GitHubUser
    ): ApiResponse<Unit> {
        commentService.deleteComment(commentId, githubUser)
        return ApiResponse.success(Unit)
    }
}