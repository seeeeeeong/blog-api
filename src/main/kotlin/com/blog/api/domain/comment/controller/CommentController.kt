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
        @RequestHeader("User-Id") userId: Long,
        @Valid @RequestBody request: CreateCommentRequest
    ): ResponseEntity<CommentResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(postId, userId, request))
    }
    
    @GetMapping
    fun getComments(@PathVariable postId: Long): ResponseEntity<List<CommentResponse>> {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId))
    }
    
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @RequestHeader("User-Id") userId: Long,
        @Valid @RequestBody request: UpdateCommentRequest
    ): ResponseEntity<CommentResponse> {
        return ResponseEntity.ok(commentService.updateComment(commentId, userId, request))
    }
    
    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @RequestHeader("User-Id") userId: Long
    ): ResponseEntity<Void> {
        commentService.deleteComment(commentId, userId)
        return ResponseEntity.noContent().build()
    }
}
