package com.blog.api.core.domain.comment

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.converter.MarkdownRenderer
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.comment.CommentEntity
import com.blog.api.storage.comment.CommentRepository
import com.blog.api.storage.post.PostRepository
import com.blog.api.storage.comment.toComment
import com.blog.api.storage.comment.toCommentWithReplies
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val postMarkdownConverter: PostMarkdownConverter,
    private val markdownRenderer: MarkdownRenderer,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun createComment(commentCreate: CommentCreate): Comment {
        requirePublishedPost(commentCreate.postId)

        val contentHtml = postMarkdownConverter.convertToHtml(commentCreate.content)
        val saved = commentRepository.save(
            CommentEntity(
                postId = commentCreate.postId,
                nickname = commentCreate.nickname,
                password = passwordEncoder.encode(commentCreate.password),
                parentId = resolveParentId(commentCreate.parentId, commentCreate.postId),
                content = commentCreate.content,
                contentHtml = contentHtml,
            )
        )

        return saved.toComment()
    }

    fun getCommentsByPost(postId: Long): List<CommentWithReplies> {
        val rootComments = commentRepository.findRootCommentsByPostId(postId)
        if (rootComments.isEmpty()) return emptyList()

        val replies = commentRepository.findReplyCommentsByPostId(postId)
        val repliesByParent = buildRepliesByParent(replies)
        return rootComments.map { it.toCommentWithReplies(repliesByParent) }
            .map { ensureContentHtml(it) }
    }

    @Transactional
    fun updateComment(postId: Long, commentId: Long, commentUpdate: CommentUpdate): Comment {
        val comment = getActiveComment(commentId)
        requireBelongsToPost(comment, postId)
        validatePassword(commentUpdate.password, comment.password)
        comment.updateContent(commentUpdate.content, postMarkdownConverter.convertToHtml(commentUpdate.content))
        return comment.toComment()
    }

    @Transactional
    fun deleteComment(postId: Long, commentId: Long, password: String) {
        val comment = getActiveComment(commentId)
        requireBelongsToPost(comment, postId)
        validatePassword(password, comment.password)
        comment.delete()
    }

    @Transactional
    fun deleteCommentByAdmin(postId: Long, commentId: Long) {
        val comment = getActiveComment(commentId)
        requireBelongsToPost(comment, postId)
        comment.delete()
    }

    private fun resolveParentId(parentId: Long?, postId: Long): Long? {
        if (parentId == null) return null
        val parentComment = getActiveComment(parentId)
        if (parentComment.postId == postId) return parentId
        throw CoreException(ErrorType.COMMENT_NOT_FOUND)
    }

    private fun requirePublishedPost(postId: Long) {
        val exists = postRepository.existsByIdAndStatus(postId, PostStatus.PUBLISHED)
        if (exists) return
        throw CoreException(ErrorType.POST_NOT_FOUND)
    }

    private fun buildRepliesByParent(replies: List<CommentEntity>): Map<Long, List<Comment>> =
        replies.groupBy(
            keySelector = { requireNotNull(it.parentId) { "Reply comment must have a parentId" } },
            valueTransform = { renderHtmlIfMissing(it.toComment()) },
        )

    private fun getActiveComment(commentId: Long): CommentEntity =
        commentRepository.findByIdOrNull(commentId)
            ?.takeIf { it.isActive() }
            ?: throw CoreException(ErrorType.COMMENT_NOT_FOUND)

    private fun requireBelongsToPost(comment: CommentEntity, postId: Long) {
        if (comment.postId == postId) return
        throw CoreException(ErrorType.COMMENT_NOT_FOUND)
    }

    private fun validatePassword(rawPassword: String, encodedPassword: String) {
        val isValid = passwordEncoder.matches(rawPassword, encodedPassword)
        if (isValid) return
        throw CoreException(ErrorType.INVALID_PASSWORD)
    }

    private fun renderHtmlIfMissing(comment: Comment): Comment {
        val hasRenderedHtml = comment.contentHtml != comment.content
        if (hasRenderedHtml) return comment
        return comment.copy(contentHtml = renderMarkdownOrEscape(comment.id, comment.content))
    }

    private fun ensureContentHtml(commentWithReplies: CommentWithReplies): CommentWithReplies {
        val rendered = renderHtmlIfMissing(commentWithReplies.comment)
        if (rendered === commentWithReplies.comment) return commentWithReplies
        return commentWithReplies.copy(comment = rendered)
    }

    private fun renderMarkdownOrEscape(commentId: Long, content: String): String =
        markdownRenderer.renderOrEscape(content, commentId)
}
