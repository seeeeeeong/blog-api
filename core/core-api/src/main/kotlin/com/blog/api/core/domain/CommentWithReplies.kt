package com.blog.api.core.domain

data class CommentWithReplies(
    val comment: Comment,
    val replies: List<Comment>
)
