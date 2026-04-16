package com.blog.api.core.domain.comment

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.comment.CommentEntity
import com.blog.api.storage.comment.CommentRepository
import com.blog.api.storage.post.PostRepository
import com.blog.api.storage.comment.toComment
import com.blog.api.storage.comment.toCommentWithReplies
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.HtmlUtils

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val postMarkdownConverter: PostMarkdownConverter,
    private val passwordEncoder: PasswordEncoder,
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

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
        validatePostId(comment, postId)
        validatePassword(commentUpdate.password, comment.password)
        comment.updateContent(commentUpdate.content, postMarkdownConverter.convertToHtml(commentUpdate.content))
        return comment.toComment()
    }

    @Transactional
    fun deleteComment(postId: Long, commentId: Long, password: String) {
        val comment = getActiveComment(commentId)
        validatePostId(comment, postId)
        validatePassword(password, comment.password)
        comment.delete()
    }

    @Transactional
    fun deleteCommentByAdmin(postId: Long, commentId: Long) {
        val comment = getActiveComment(commentId)
        validatePostId(comment, postId)
        comment.delete()
    }

    private fun resolveParentId(parentId: Long?, postId: Long): Long? {
        if (parentId == null) {
            return null
        }

        val parentComment = getActiveComment(parentId)
        if (parentComment.postId == postId) {
            return parentId
        }
        throw CoreException(ErrorType.COMMENT_NOT_FOUND)
    }

    private fun requirePublishedPost(postId: Long) {
        val exists = postRepository.existsByIdAndStatus(postId, PostStatus.PUBLISHED)
        if (exists) return
        throw CoreException(ErrorType.POST_NOT_FOUND)
    }

    private fun buildRepliesByParent(replies: List<CommentEntity>): Map<Long, List<Comment>> {
        val result = mutableMapOf<Long, MutableList<Comment>>()
        for (reply in replies) {
            val parentId = requireNotNull(reply.parentId) { "Reply comment must have a parentId" }
            result.getOrPut(parentId) { mutableListOf() }
                .add(renderHtmlIfMissing(reply.toComment()))
        }
        return result
    }

    private fun getActiveComment(commentId: Long): CommentEntity {
        return commentRepository.findById(commentId)
            .filter { it.isActive() }
            .orElseThrow { CoreException(ErrorType.COMMENT_NOT_FOUND) }
    }

    private fun validatePostId(comment: CommentEntity, postId: Long) {
        if (comment.postId != postId) throw CoreException(ErrorType.COMMENT_NOT_FOUND)
    }

    private fun validatePassword(rawPassword: String, encodedPassword: String) {
        val isValid = passwordEncoder.matches(rawPassword, encodedPassword)
        if (isValid) return
        throw CoreException(ErrorType.INVALID_PASSWORD)
    }

    private fun renderHtmlIfMissing(comment: Comment): Comment {
        if (comment.contentHtml != comment.content) return comment
        return comment.copy(contentHtml = renderMarkdownOrEscape(comment.id, comment.content))
    }

    private fun ensureContentHtml(commentWithReplies: CommentWithReplies): CommentWithReplies {
        val rendered = renderHtmlIfMissing(commentWithReplies.comment)
        return if (rendered === commentWithReplies.comment) commentWithReplies
        else commentWithReplies.copy(comment = rendered)
    }

    private fun renderMarkdownOrEscape(commentId: Long, content: String): String {
        return try {
            postMarkdownConverter.convertToHtml(content)
        } catch (e: Exception) {
            log.warn(e) { "Comment markdown conversion failed: commentId=$commentId" }
            HtmlUtils.htmlEscape(content)
        }
    }
}
