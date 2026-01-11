package com.blog.api.core.api.controller.v1

import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.support.auth.OAuthAuth
import com.blog.api.core.api.controller.v1.reqeust.CommentCreateRequest
import com.blog.api.core.api.controller.v1.reqeust.CommentUpdateRequest
import com.blog.api.core.api.controller.v1.response.CommentResponse
import com.blog.api.core.domain.OAuthUser
import com.blog.api.core.domain.CommentWithReplies
import com.blog.api.core.domain.CommentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

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
        @OAuthAuth oauthUser: OAuthUser,
        @RequestBody request: CommentCreateRequest
    ): ApiResponse<CommentResponse> {
        val comment = commentService.createComment(request.toCommentCreate(postId, oauthUser))
        return ApiResponse.Companion.success(CommentResponse.Companion.of(CommentWithReplies(comment, emptyList())))
    }

    @Operation(summary = "댓글 목록 조회")
    @GetMapping
    fun getComments(
        @PathVariable postId: Long
    ): ApiResponse<List<CommentResponse>> {
        val comments = commentService.getCommentsByPost(postId)
        return ApiResponse.Companion.success(comments.map(CommentResponse.Companion::of))
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @OAuthAuth oauthUser: OAuthUser,
        @RequestBody request: CommentUpdateRequest
    ): ApiResponse<CommentResponse> {
        val comment = commentService.updateComment(commentId, oauthUser.id.toString(), request.toCommentUpdate())
        return ApiResponse.Companion.success(CommentResponse.Companion.of(CommentWithReplies(comment, emptyList())))
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @OAuthAuth oauthUser: OAuthUser
    ): ApiResponse<Any> {
        commentService.deleteComment(commentId, oauthUser.id.toString())
        return ApiResponse.Companion.success()
    }
}