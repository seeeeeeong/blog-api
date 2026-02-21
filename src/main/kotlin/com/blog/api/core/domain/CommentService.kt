package com.blog.api.core.domain

import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.CommentEntity
import com.blog.api.storage.CommentRepository
import com.blog.api.storage.PostRepository
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

        val saved = commentRepository.save(
            CommentEntity(
                postId = commentCreate.postId,
                oauthId = commentCreate.oauthId,
                oauthUsername = commentCreate.oauthUsername,
                oauthAvatarUrl = commentCreate.oauthAvatarUrl.takeIf { it.isNotBlank() },
                parentId = resolveParentId(commentCreate.parentId, commentCreate.postId),
                content = commentCreate.content,
            )
        )

        return toComment(saved)
    }

    fun getCommentsByPost(postId: Long): List<CommentWithReplies> {
        val comments = commentRepository.findAllByPostId(postId)
        if (comments.isEmpty()) {
            return emptyList()
        }

        val repliesByParent = buildRepliesByParent(comments)
        val rootComments = findRootComments(comments)
        return rootComments.map { toCommentWithReplies(it, repliesByParent) }
    }

    @Transactional
    fun updateComment(commentId: Long, oauthId: String, commentUpdate: CommentUpdate): Comment {
        val comment = findActiveComment(commentId)
        validateOwner(comment, oauthId)
        comment.updateContent(commentUpdate.content)
        return toComment(comment)
    }

    @Transactional
    fun deleteComment(commentId: Long, oauthId: String) {
        val comment = findActiveComment(commentId)
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
        postRepository.findById(postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }
    }

    private fun buildRepliesByParent(comments: List<CommentEntity>): Map<Long, List<Comment>> {
        return comments
            .filter { it.parentId != null }
            .groupBy { it.parentId!! }
            .mapValues { (_, groupedComments) ->
                groupedComments
                    .sortedBy { it.createdAt }
                    .map { toComment(it) }
            }
    }

    private fun findRootComments(comments: List<CommentEntity>): List<CommentEntity> {
        return comments
            .filter { it.parentId == null }
            .sortedByDescending { it.createdAt }
    }

    private fun toCommentWithReplies(
        rootComment: CommentEntity,
        repliesByParent: Map<Long, List<Comment>>,
    ): CommentWithReplies {
        val rootCommentId = rootComment.id ?: throw CoreException(ErrorType.COMMENT_NOT_FOUND)
        val replies = repliesByParent[rootCommentId].orEmpty()
        return CommentWithReplies(
            comment = toComment(rootComment),
            replies = replies,
        )
    }

    private fun findActiveComment(commentId: Long): CommentEntity {
        return commentRepository.findById(commentId)
            .filter { it.isActive() }
            .orElseThrow { CoreException(ErrorType.COMMENT_NOT_FOUND) }
    }

    private fun validateOwner(comment: CommentEntity, oauthId: String) {
        if (comment.oauthId == oauthId) {
            return
        }
        throw CoreException(ErrorType.FORBIDDEN)
    }

    private fun toComment(entity: CommentEntity): Comment {
        return Comment(
            id = entity.id!!,
            postId = entity.postId,
            oauthId = entity.oauthId,
            oauthUsername = entity.oauthUsername,
            oauthAvatarUrl = entity.oauthAvatarUrl ?: "",
            parentId = entity.parentId ?: 0L,
            content = entity.content,
            contentHtml = postMarkdownConverter.convertToHtml(entity.content),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
