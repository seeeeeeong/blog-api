package com.blog.api.domain.comment.repository

import com.blog.api.domain.comment.entity.Comment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<Comment, Long> {

    @Query(
        value = """
            SELECT * FROM comments
            WHERE post_id = :postId
            AND parent_id IS NULL
            ORDER BY created_at DESC
        """,
        nativeQuery = true
    )
    fun findParentComments(@Param("postId") postId: Long): List<Comment>

    @Query(
        value = """
            SELECT * FROM comments
            WHERE parent_id = :parentId
            ORDER BY created_at ASC
        """,
        nativeQuery = true
    )
    fun findReplies(@Param("parentId") parentId: Long): List<Comment>
}
