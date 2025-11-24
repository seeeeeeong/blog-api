package com.blog.api.domain.comment.service

import com.blog.api.domain.comment.dto.CommentResponse
import com.blog.api.domain.comment.dto.CreateCommentRequest
import com.blog.api.domain.comment.dto.UpdateCommentRequest
import com.blog.api.domain.comment.entity.Comment
import com.blog.api.domain.comment.repository.CommentRepository
import com.blog.api.domain.post.service.PostService
import com.blog.api.global.web.dto.GitHubUser
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postService: PostService
) {

    @Transactional
    fun createComment(
        postId: Long,
        githubUser: GitHubUser,
        request: CreateCommentRequest
    ): CommentResponse {

        validatePost(postId)
        validateParentComment(request.parentId)

        val comment = Comment(
            postId = postId,
            githubId = githubUser.githubId,
            githubUsername = githubUser.githubUsername,
            githubAvatarUrl = githubUser.githubAvatarUrl,
            parentId = request.parentId,
            content = request.content
        )

        return CommentResponse.from(commentRepository.save(comment))
    }

    fun getCommentsByPost(postId: Long): List<CommentResponse> {
        val parentComments =
            commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(postId)

        return parentComments.map { parent ->
            val replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parent.id!!)
            CommentResponse.fromWithReplies(parent, replies)
        }
    }

    @Transactional
    fun updateComment(
        commentId: Long,
        githubUser: GitHubUser,
        request: UpdateCommentRequest
    ): CommentResponse {
        val comment = findCommentById(commentId)
        validateCommentOwner(comment, githubUser.githubId)

        comment.content = request.content
        return CommentResponse.from(comment)
    }

    @Transactional
    fun deleteComment(commentId: Long, githubUser: GitHubUser) {
        val comment = findCommentById(commentId)
        validateCommentOwner(comment, githubUser.githubId)
        commentRepository.delete(comment)
    }

    private fun validatePost(postId: Long) {
        postService.validatePostExists(postId)
    }

    private fun validateParentComment(parentId: Long?) {
        if (parentId == null) return

        val parentExists = commentRepository.existsById(parentId)
        if (parentExists) return

        throw CustomException(ErrorCode.COMMENT_NOT_FOUND)
    }

    private fun findCommentById(commentId: Long): Comment {
        return commentRepository.findById(commentId)
            .orElseThrow { CustomException(ErrorCode.COMMENT_NOT_FOUND) }
    }

    private fun validateCommentOwner(comment: Comment, githubId: String) {
        val isOwner = (comment.githubId == githubId)
        if (isOwner) return

        throw CustomException(ErrorCode.FORBIDDEN)
    }
}
