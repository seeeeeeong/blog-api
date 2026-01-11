package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.CommentRepository
import com.blog.api.storage.CommentEntity
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

    @Transactional
    fun createComment(commentCreate: CommentCreate): Comment {
        postRepository.findById(commentCreate.postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

        val parentId = commentCreate.parentId
        val validatedParentId = if (parentId == 0L) {
            null
        } else {
            commentRepository.findById(parentId)
                .filter { it.isActive() && it.postId == commentCreate.postId }
                .orElseThrow { CoreException(ErrorType.COMMENT_NOT_FOUND) }
            parentId
        }

        val savedEntity = commentRepository.save(
            CommentEntity(
                postId = commentCreate.postId,
                oauthId = commentCreate.oauthId,
                oauthUsername = commentCreate.oauthUsername,
                oauthAvatarUrl = commentCreate.oauthAvatarUrl.takeIf { it.isNotBlank() },
                parentId = validatedParentId,
                content = commentCreate.content,
            )
        )

        val contentHtml = postMarkdownConverter.convertToHtml(savedEntity.content)
        return Comment(
            id = savedEntity.id!!,
            postId = savedEntity.postId,
            oauthId = savedEntity.oauthId,
            oauthUsername = savedEntity.oauthUsername,
            oauthAvatarUrl = savedEntity.oauthAvatarUrl ?: "",
            parentId = savedEntity.parentId ?: 0L,
            content = savedEntity.content,
            contentHtml = contentHtml,
            createdAt = savedEntity.createdAt,
            updatedAt = savedEntity.updatedAt,
        )
    }

    fun getCommentsByPost(postId: Long): List<CommentWithReplies> {
        val allComments = commentRepository.findAllByPostId(postId)
        if (allComments.isEmpty()) return emptyList()

        val parentComments = allComments
            .filter { it.parentId == null }
            .sortedByDescending { it.createdAt }

        val parentCommentIds = parentComments.mapNotNull { it.id }.toSet()
        val repliesByParentId = allComments
            .filter { it.parentId in parentCommentIds }
            .groupBy { it.parentId!! }
            .mapValues { (_, replies) ->
                replies.sortedBy { it.createdAt }.map { entity ->
                    val contentHtml = postMarkdownConverter.convertToHtml(entity.content)
                    Comment(
                        id = entity.id!!,
                        postId = entity.postId,
                        oauthId = entity.oauthId,
                        oauthUsername = entity.oauthUsername,
                        oauthAvatarUrl = entity.oauthAvatarUrl ?: "",
                        parentId = entity.parentId ?: 0L,
                        content = entity.content,
                        contentHtml = contentHtml,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                    )
                }
            }

        return parentComments.map { parent ->
            val contentHtml = postMarkdownConverter.convertToHtml(parent.content)
            val parentComment = Comment(
                id = parent.id!!,
                postId = parent.postId,
                oauthId = parent.oauthId,
                oauthUsername = parent.oauthUsername,
                oauthAvatarUrl = parent.oauthAvatarUrl ?: "",
                parentId = parent.parentId ?: 0L,
                content = parent.content,
                contentHtml = contentHtml,
                createdAt = parent.createdAt,
                updatedAt = parent.updatedAt,
            )

            CommentWithReplies(
                comment = parentComment,
                replies = repliesByParentId[parent.id] ?: emptyList(),
            )
        }
    }

    @Transactional
    fun updateComment(commentId: Long, oauthId: String, commentUpdate: CommentUpdate): Comment {
        val comment = commentRepository.findById(commentId)
            .filter { it.isActive() }
            .orElseThrow { CoreException(ErrorType.COMMENT_NOT_FOUND) }

        if (comment.oauthId == oauthId) {
            comment.updateContent(commentUpdate.content)

            val contentHtml = postMarkdownConverter.convertToHtml(comment.content)
            return Comment(
                id = comment.id!!,
                postId = comment.postId,
                oauthId = comment.oauthId,
                oauthUsername = comment.oauthUsername,
                oauthAvatarUrl = comment.oauthAvatarUrl ?: "",
                parentId = comment.parentId ?: 0L,
                content = comment.content,
                contentHtml = contentHtml,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
            )
        }

        throw CoreException(ErrorType.FORBIDDEN)
    }

    @Transactional
    fun deleteComment(commentId: Long, oauthId: String) {
        val comment = commentRepository.findById(commentId)
            .filter { it.isActive() }
            .orElseThrow { CoreException(ErrorType.COMMENT_NOT_FOUND) }

        if (comment.oauthId == oauthId) {
            comment.delete()
            return
        }

        throw CoreException(ErrorType.FORBIDDEN)
    }
}
