package com.blog.api.storage.comment

import com.blog.api.core.domain.comment.Comment
import com.blog.api.core.domain.comment.CommentWithReplies
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.util.HtmlUtils

private val log = KotlinLogging.logger {}

fun CommentEntity.toComment(converter: PostMarkdownConverter): Comment = Comment(
    id = requireNotNull(id) { "CommentEntity.id must not be null after persistence" },
    postId = postId,
    nickname = nickname,
    parentId = parentId,
    content = content,
    contentHtml = contentHtml ?: convertToHtmlSafely(converter, id, content),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun convertToHtmlSafely(converter: PostMarkdownConverter, commentId: Long?, content: String): String {
    return try {
        converter.convertToHtml(content)
    } catch (e: Exception) {
        log.warn(e) { "Comment markdown conversion failed: commentId=$commentId" }
        HtmlUtils.htmlEscape(content)
    }
}

fun CommentEntity.toCommentWithReplies(
    repliesByParent: Map<Long, List<Comment>>,
    converter: PostMarkdownConverter,
): CommentWithReplies {
    val rootCommentId = id ?: throw CoreException(ErrorType.COMMENT_NOT_FOUND)
    val replies = repliesByParent[rootCommentId].orEmpty()
    return CommentWithReplies(
        comment = toComment(converter),
        replies = replies,
    )
}
