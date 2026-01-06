package com.blog.api.storage.db.core

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<CommentEntity, Long> {

    @Query(
        value = """
            SELECT * FROM comments
            WHERE post_id = :postId
            AND is_deleted = false
            ORDER BY created_at ASC
        """,
        nativeQuery = true
    )
    fun findAllByPostId(@Param("postId") postId: Long): List<CommentEntity>
}
