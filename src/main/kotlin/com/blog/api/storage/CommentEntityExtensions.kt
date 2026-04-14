package com.blog.api.storage

import com.blog.api.core.domain.Comment
import com.blog.api.core.domain.CommentWithReplies
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType

fun CommentEntity.toComment(converter: PostMarkdownConverter): Comment = Comment(
    id = id!!,
    postId = postId,
    nickname = nickname,
    parentId = parentId,
    content = content,
    contentHtml = contentHtml ?: converter.convertToHtml(content),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

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
