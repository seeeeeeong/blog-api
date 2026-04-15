package com.blog.api.core.domain.comment

data class CommentWithReplies(
    val comment: Comment,
    val replies: List<Comment>
)
