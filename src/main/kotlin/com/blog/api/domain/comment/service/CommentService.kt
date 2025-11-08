package com.blog.api.domain.comment.service

import com.blog.api.domain.comment.dto.CommentResponse
import com.blog.api.domain.comment.dto.CreateCommentRequest
import com.blog.api.domain.comment.dto.UpdateCommentRequest
import com.blog.api.domain.comment.entity.Comment
import com.blog.api.domain.comment.repository.CommentRepository
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository
) {
    
    @Transactional
    fun createComment(postId: Long, userId: Long, request: CreateCommentRequest): CommentResponse {
        postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }
        
        request.parentId?.let { parentId ->
            commentRepository.findById(parentId)
                .orElseThrow { CustomException(ErrorCode.COMMENT_NOT_FOUND) }
        }
        
        val comment = Comment(
            postId = postId,
            userId = userId,
            parentId = request.parentId,
            content = request.content
        )
        
        return commentRepository.save(comment).let { CommentResponse.from(it) }
    }
    
    fun getCommentsByPost(postId: Long): List<CommentResponse> {
        val parentComments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(postId)
        
        return parentComments.map { parent ->
            val replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parent.id!!)
            CommentResponse.fromWithReplies(parent, replies)
        }
    }
    
    @Transactional
    fun updateComment(commentId: Long, userId: Long, request: UpdateCommentRequest): CommentResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { CustomException(ErrorCode.COMMENT_NOT_FOUND) }
        
        (comment.userId == userId)
            .takeIf { it }
            ?: throw CustomException(ErrorCode.FORBIDDEN)
        
        comment.content = request.content
        
        return CommentResponse.from(comment)
    }
    
    @Transactional
    fun deleteComment(commentId: Long, userId: Long) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { CustomException(ErrorCode.COMMENT_NOT_FOUND) }
        
        (comment.userId == userId)
            .takeIf { it }
            ?: throw CustomException(ErrorCode.FORBIDDEN)
        
        commentRepository.delete(comment)
    }
}
