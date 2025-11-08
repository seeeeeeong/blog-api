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
        val comment = commentService.createComment(postId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(comment)
    }
    
    @GetMapping
    fun getComments(@PathVariable postId: Long): ResponseEntity<List<CommentResponse>> {
        val comments = commentService.getCommentsByPost(postId)
        return ResponseEntity.ok(comments)
    }
    
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @RequestHeader("User-Id") userId: Long,
        @Valid @RequestBody request: UpdateCommentRequest
    ): ResponseEntity<CommentResponse> {
        val comment = commentService.updateComment(commentId, userId, request)
        return ResponseEntity.ok(comment)
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
