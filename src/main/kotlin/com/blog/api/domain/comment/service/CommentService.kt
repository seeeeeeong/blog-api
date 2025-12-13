package com.blog.api.domain.comment.service

import com.blog.api.domain.comment.dto.CommentResponse
import com.blog.api.domain.comment.dto.CreateCommentRequest
import com.blog.api.domain.comment.dto.UpdateCommentRequest
import com.blog.api.domain.comment.entity.Comment
import com.blog.api.domain.comment.repository.CommentRepository
import com.blog.api.domain.post.service.PostService
import com.blog.api.infrastructure.oauth.dto.GitHubUser
import com.blog.api.common.exception.CustomException
import com.blog.api.common.exception.ErrorCode
import com.blog.api.common.util.MarkdownUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postService: PostService,
    private val markdownUtil: MarkdownUtil
) {

    @Transactional
    fun createComment(
        postId: Long,
        githubUser: GitHubUser,
        request: CreateCommentRequest
    ): CommentResponse {
        postService.validatePostExists(postId)
        validateParentComment(request.parentId)

        val comment = Comment(
            postId = postId,
            githubId = githubUser.githubId,
            githubUsername = githubUser.githubUsername,
            githubAvatarUrl = githubUser.githubAvatarUrl,
            parentId = request.parentId,
            content = request.content
        )

        val savedComment = commentRepository.save(comment)
        return CommentResponse.from(savedComment, markdownUtil.convertToHtml(savedComment.content))
    }

    fun getCommentsByPost(postId: Long): List<CommentResponse> {
        val parentComments = commentRepository.findParentComments(postId)
        return parentComments.map { getCommentWithReplies(it) }
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

        return CommentResponse.from(comment, markdownUtil.convertToHtml(comment.content))
    }

    @Transactional
    fun deleteComment(commentId: Long, githubUser: GitHubUser) {
        val comment = findCommentById(commentId)
        validateCommentOwner(comment, githubUser.githubId)

        commentRepository.delete(comment)
    }

    private fun findCommentById(commentId: Long): Comment =
        commentRepository.findById(commentId)
            .orElseThrow { CustomException(ErrorCode.COMMENT_NOT_FOUND) }

    private fun validateParentComment(parentId: Long?) {
        parentId?.takeIf { commentRepository.existsById(it) }
            ?: parentId?.let { throw CustomException(ErrorCode.COMMENT_NOT_FOUND) }
    }

    private fun validateCommentOwner(comment: Comment, githubId: String) {
        comment.takeIf { isCommentOwner(it, githubId) }
            ?: throw CustomException(ErrorCode.FORBIDDEN)
    }

    private fun isCommentOwner(comment: Comment, githubId: String): Boolean =
        comment.githubId == githubId

    private fun getCommentWithReplies(parent: Comment): CommentResponse {
        val replies = commentRepository.findReplies(parent.id!!)
        return CommentResponse.fromWithReplies(parent, replies, markdownUtil::convertToHtml)
    }
}
