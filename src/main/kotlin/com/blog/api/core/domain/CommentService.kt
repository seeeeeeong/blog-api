package com.blog.api.core.domain

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.CommentEntity
import com.blog.api.storage.CommentRepository
import com.blog.api.storage.PostRepository
import com.blog.api.storage.toComment
import com.blog.api.storage.toCommentWithReplies
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val postMarkdownConverter: PostMarkdownConverter,
) {
    companion object {
        private const val ROOT_PARENT_ID = 0L
    }

    @Transactional
    fun createComment(commentCreate: CommentCreate): Comment {
        ensurePostExists(commentCreate.postId)

        val contentHtml = postMarkdownConverter.convertToHtml(commentCreate.content)
        val saved = commentRepository.save(
            CommentEntity(
                postId = commentCreate.postId,
                oauthId = commentCreate.oauthId,
                oauthUsername = commentCreate.oauthUsername,
                oauthAvatarUrl = commentCreate.oauthAvatarUrl.takeIf { it.isNotBlank() },
                parentId = resolveParentId(commentCreate.parentId, commentCreate.postId),
                content = commentCreate.content,
                contentHtml = contentHtml,
            )
        )

        return saved.toComment(postMarkdownConverter)
    }

    fun getCommentsByPost(postId: Long): List<CommentWithReplies> {
        val rootComments = commentRepository.findRootCommentsByPostId(postId)
        if (rootComments.isEmpty()) return emptyList()

        val replies = commentRepository.findReplyCommentsByPostId(postId)
        val repliesByParent = buildRepliesByParent(replies)
        return rootComments.map { it.toCommentWithReplies(repliesByParent, postMarkdownConverter) }
    }

    @Transactional
    fun updateComment(postId: Long, commentId: Long, oauthId: String, commentUpdate: CommentUpdate): Comment {
        val comment = findActiveComment(commentId)
        validatePostId(comment, postId)
        validateOwner(comment, oauthId)
        comment.updateContent(commentUpdate.content, postMarkdownConverter.convertToHtml(commentUpdate.content))
        return comment.toComment(postMarkdownConverter)
    }

    @Transactional
    fun deleteComment(postId: Long, commentId: Long, oauthId: String) {
        val comment = findActiveComment(commentId)
        validatePostId(comment, postId)
        validateOwner(comment, oauthId)
        comment.delete()
    }

    private fun resolveParentId(parentId: Long, postId: Long): Long? {
        if (parentId == ROOT_PARENT_ID) {
            return null
        }

        val parentComment = findActiveComment(parentId)
        if (parentComment.postId == postId) {
            return parentId
        }
        throw CoreException(ErrorType.COMMENT_NOT_FOUND)
    }

    private fun ensurePostExists(postId: Long) {
        val exists = postRepository.existsByIdAndStatus(postId, PostStatus.PUBLISHED)
        if (!exists) throw CoreException(ErrorType.POST_NOT_FOUND)
    }

    private fun buildRepliesByParent(replies: List<CommentEntity>): Map<Long, List<Comment>> {
        val result = mutableMapOf<Long, MutableList<Comment>>()
        for (reply in replies) {
            result.getOrPut(reply.parentId!!) { mutableListOf() }
                .add(reply.toComment(postMarkdownConverter))
        }
        return result
    }

    private fun findActiveComment(commentId: Long): CommentEntity {
        return commentRepository.findById(commentId)
            .filter { it.isActive() }
            .orElseThrow { CoreException(ErrorType.COMMENT_NOT_FOUND) }
    }

    private fun validatePostId(comment: CommentEntity, postId: Long) {
        if (comment.postId != postId) throw CoreException(ErrorType.COMMENT_NOT_FOUND)
    }

    private fun validateOwner(comment: CommentEntity, oauthId: String) {
        if (comment.oauthId != oauthId) throw CoreException(ErrorType.FORBIDDEN)
    }

}
