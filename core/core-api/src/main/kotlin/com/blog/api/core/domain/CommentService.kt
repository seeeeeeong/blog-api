package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.util.MarkdownUtil
import com.blog.api.storage.db.core.CommentEntity
import com.blog.api.storage.db.core.CommentRepository
import com.blog.api.storage.db.core.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val markdownUtil: MarkdownUtil
) {

    @Transactional
    fun createComment(commentCreate: CommentCreate): Comment {
        postRepository.findById(commentCreate.postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

        val parentId = commentCreate.parentId.takeIf { it != 0L }
        if (parentId != null) {
            val parent = commentRepository.findById(parentId)
                .orElseThrow { CoreException(ErrorType.COMMENT_NOT_FOUND) }
                .takeIf { it.isActive() }
                ?: throw CoreException(ErrorType.COMMENT_NOT_FOUND)

            if (parent.postId != commentCreate.postId) {
                throw CoreException(ErrorType.COMMENT_NOT_FOUND)
            }
        }

        val entity = CommentEntity(
            postId = commentCreate.postId,
            githubId = commentCreate.githubId,
            githubUsername = commentCreate.githubUsername,
            githubAvatarUrl = commentCreate.githubAvatarUrl
                .takeIf { it.isNotBlank() },
            parentId = parentId,
            content = commentCreate.content
        )

        val saved = commentRepository.save(entity)
        return toComment(saved)
    }

    fun getCommentsByPost(postId: Long): List<CommentWithReplies> {
        val comments = commentRepository.findAllByPostId(postId)
        if (comments.isEmpty()) {
            return emptyList()
        }

        val parents = comments
            .filter { it.parentId == null }
            .sortedByDescending { it.createdAt }

        val parentIds = parents.mapNotNull { it.id }.toSet()
        val repliesByParent = comments.asSequence()
            .filter { it.parentId != null && parentIds.contains(it.parentId) }
            .groupBy { it.parentId!! }
            .mapValues { (_, replies) -> replies.sortedBy { it.createdAt } }

        return parents.map { parent ->
            val replies = repliesByParent[parent.id!!].orEmpty().map { toComment(it) }
            CommentWithReplies(
                comment = toComment(parent),
                replies = replies
            )
        }
    }

    @Transactional
    fun updateComment(
        commentId: Long,
        githubId: String,
        commentUpdate: CommentUpdate
    ): Comment {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { CoreException(ErrorType.COMMENT_NOT_FOUND) }
            .takeIf { it.isActive() }
            ?: throw CoreException(ErrorType.COMMENT_NOT_FOUND)

        if (comment.githubId != githubId) {
            throw CoreException(ErrorType.FORBIDDEN)
        }

        comment.updateContent(commentUpdate.content)
        return toComment(comment)
    }

    @Transactional
    fun deleteComment(commentId: Long, githubId: String) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { CoreException(ErrorType.COMMENT_NOT_FOUND) }
            .takeIf { it.isActive() }
            ?: throw CoreException(ErrorType.COMMENT_NOT_FOUND)

        if (comment.githubId != githubId) {
            throw CoreException(ErrorType.FORBIDDEN)
        }

        comment.delete()
    }

    private fun toComment(entity: CommentEntity): Comment {
        val contentHtml = markdownUtil.convertToHtml(entity.content)

        return Comment(
            id = entity.id!!,
            postId = entity.postId,
            githubId = entity.githubId,
            githubUsername = entity.githubUsername,
            githubAvatarUrl = entity.githubAvatarUrl ?: "",
            parentId = entity.parentId ?: 0L,
            content = entity.content,
            contentHtml = contentHtml,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
