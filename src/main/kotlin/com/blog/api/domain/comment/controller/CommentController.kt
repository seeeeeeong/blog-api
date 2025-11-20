package com.blog.api.domain.comment.controller

import com.blog.api.domain.comment.dto.CommentResponse
import com.blog.api.domain.comment.dto.CreateCommentRequest
import com.blog.api.domain.comment.dto.UpdateCommentRequest
import com.blog.api.domain.comment.service.CommentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts/{postId}/comments")
class CommentController(
    private val commentService: CommentService
) {

    @PostMapping
    fun createComment(
        @PathVariable postId: Long,
        @RequestHeader("Authorization") authorization: String,
        @RequestHeader("GitHub-Username") githubUsername: String,
        @RequestHeader("GitHub-Avatar-Url", required = false) githubAvatarUrl: String?,
        @Valid @RequestBody request: CreateCommentRequest
    ): ResponseEntity<CommentResponse> {
        val token = authorization.removePrefix("Bearer ")
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(commentService.createComment(postId, token, githubUsername, githubAvatarUrl, request))
    }

    @GetMapping
    fun getComments(@PathVariable postId: Long): ResponseEntity<List<CommentResponse>> {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId))
    }

    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @RequestHeader("Authorization") authorization: String,
        @Valid @RequestBody request: UpdateCommentRequest
    ): ResponseEntity<CommentResponse> {
        val token = authorization.removePrefix("Bearer ")
        return ResponseEntity.ok(commentService.updateComment(commentId, token, request))
    }

    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Void> {
        val token = authorization.removePrefix("Bearer ")
        commentService.deleteComment(commentId, token)
        return ResponseEntity.noContent().build()
    }
}