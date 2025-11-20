package com.blog.api.domain.comment.service

import com.blog.api.domain.comment.dto.CommentResponse
import com.blog.api.domain.comment.dto.CreateCommentRequest
import com.blog.api.domain.comment.dto.UpdateCommentRequest
import com.blog.api.domain.comment.entity.Comment
import com.blog.api.domain.comment.repository.CommentRepository
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import com.blog.api.global.security.JwtProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val jwtProvider: JwtProvider
) {

    @Transactional
    fun createComment(
        postId: Long,
        token: String,
        githubUsername: String,
        githubAvatarUrl: String?,
        request: CreateCommentRequest
    ): CommentResponse {
        postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }

        request.parentId?.let { parentId ->
            commentRepository.findById(parentId)
                .orElseThrow { CustomException(ErrorCode.COMMENT_NOT_FOUND) }
        }

        val githubId = jwtProvider.getGitHubIdFromToken(token)

        val comment = Comment(
            postId = postId,
            githubId = githubId,
            githubUsername = githubUsername,
            githubAvatarUrl = githubAvatarUrl,
            parentId = request.parentId,
            content = request.content
        )

        return CommentResponse.from(commentRepository.save(comment))
    }

    fun getCommentsByPost(postId: Long): List<CommentResponse> {
        val parentComments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(postId)

        return parentComments.map { parent ->
            val replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parent.id!!)
            CommentResponse.fromWithReplies(parent, replies)
        }
    }

    @Transactional
    fun updateComment(
        commentId: Long,
        token: String,
        request: UpdateCommentRequest
    ): CommentResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { CustomException(ErrorCode.COMMENT_NOT_FOUND) }

        val githubId = jwtProvider.getGitHubIdFromToken(token)

        when (comment.isAuthor(githubId)) {
            false -> throw CustomException(ErrorCode.FORBIDDEN)
            true -> comment.content = request.content
        }

        return CommentResponse.from(comment)
    }

    @Transactional
    fun deleteComment(commentId: Long, token: String) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { CustomException(ErrorCode.COMMENT_NOT_FOUND) }

        val githubId = jwtProvider.getGitHubIdFromToken(token)

        when (comment.isAuthor(githubId)) {
            false -> throw CustomException(ErrorCode.FORBIDDEN)
            true -> commentRepository.delete(comment)
        }
    }
}