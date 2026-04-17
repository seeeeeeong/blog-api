package com.blog.api.core.domain.comment

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.nickname.RandomNicknameGenerator
import com.blog.api.storage.comment.CommentEntity
import com.blog.api.storage.comment.CommentRepository
import com.blog.api.storage.comment.toComment
import com.blog.api.storage.post.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val postMarkdownConverter: PostMarkdownConverter,
    private val nicknameGenerator: RandomNicknameGenerator,
) {

    @Transactional
    fun createComment(commentCreate: CommentCreate): Comment {
        requirePublishedPost(commentCreate.postId)

        val contentHtml = postMarkdownConverter.convertToHtml(commentCreate.content)
        val saved = commentRepository.save(
            CommentEntity(
                postId = commentCreate.postId,
                nickname = nicknameGenerator.generate(),
                content = commentCreate.content,
                contentHtml = contentHtml,
            ),
        )

        return saved.toComment()
    }

    fun getCommentsByPost(postId: Long): List<Comment> {
        return commentRepository.findByPostId(postId).map { it.toComment() }
    }

    @Transactional
    fun deleteCommentByAdmin(postId: Long, commentId: Long) {
        val comment = getActiveComment(commentId)
        requireBelongsToPost(comment, postId)
        comment.delete()
    }

    private fun requirePublishedPost(postId: Long) {
        val exists = postRepository.existsByIdAndStatus(postId, PostStatus.PUBLISHED)
        if (exists) return
        throw CoreException(ErrorType.POST_NOT_FOUND)
    }

    private fun getActiveComment(commentId: Long): CommentEntity =
        commentRepository.findById(commentId).orElseThrow { CoreException(ErrorType.COMMENT_NOT_FOUND) }

    private fun requireBelongsToPost(comment: CommentEntity, postId: Long) {
        if (comment.postId == postId) return
        throw CoreException(ErrorType.COMMENT_NOT_FOUND)
    }
}
