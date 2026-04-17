package com.blog.api.storage.comment

import com.blog.api.core.domain.comment.Comment

fun CommentEntity.toComment(): Comment = Comment(
    id = requireNotNull(id) { "CommentEntity.id must not be null after persistence" },
    postId = postId,
    nickname = nickname,
    content = content,
    contentHtml = contentHtml ?: content,
    createdAt = createdAt,
)
