package com.blog.api.storage.comment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<CommentEntity, Long> {

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.parentId IS NULL AND c.deleted = false ORDER BY c.createdAt DESC")
    fun findRootCommentsByPostId(@Param("postId") postId: Long): List<CommentEntity>

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.parentId IS NOT NULL AND c.deleted = false ORDER BY c.createdAt ASC")
    fun findReplyCommentsByPostId(@Param("postId") postId: Long): List<CommentEntity>
}
