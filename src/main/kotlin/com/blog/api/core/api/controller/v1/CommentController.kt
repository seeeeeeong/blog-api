package com.blog.api.core.api.controller.v1

import com.blog.api.core.api.controller.v1.request.CommentCreateRequest
import com.blog.api.core.api.controller.v1.response.CommentResponse
import com.blog.api.core.domain.comment.CommentService
import com.blog.api.core.support.auth.Admin
import com.blog.api.core.support.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
class CommentController(
    private val commentService: CommentService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createComment(
        @PathVariable postId: Long,
        @Valid @RequestBody request: CommentCreateRequest,
    ): ApiResponse<CommentResponse> {
        val comment = commentService.createComment(request.toCommand(postId))
        return ApiResponse.success(CommentResponse.of(comment))
    }

    @GetMapping
    fun getComments(
        @PathVariable postId: Long,
    ): ApiResponse<List<CommentResponse>> {
        val comments = commentService.getCommentsByPost(postId)
        return ApiResponse.success(comments.map { CommentResponse.of(it) })
    }

    @DeleteMapping("/{commentId}/admin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCommentByAdmin(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @Suppress("UnusedParameter") @Admin userId: Long,
    ): ApiResponse<Any> {
        commentService.deleteCommentByAdmin(postId, commentId)
        return ApiResponse.success()
    }
}
