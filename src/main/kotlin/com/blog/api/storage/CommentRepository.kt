package com.blog.api.storage

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<CommentEntity, Long> {

    // 루트 댓글: 최신순 DESC
    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.parentId IS NULL AND c.isDeleted = false ORDER BY c.createdAt DESC")
    fun findRootCommentsByPostId(@Param("postId") postId: Long): List<CommentEntity>

    // 대댓글: 오래된순 ASC
    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.parentId IS NOT NULL AND c.isDeleted = false ORDER BY c.createdAt ASC")
    fun findReplyCommentsByPostId(@Param("postId") postId: Long): List<CommentEntity>

    @Query("SELECT c FROM CommentEntity c WHERE c.contentHtml IS NULL AND c.isDeleted = false")
    fun findAllWithNullHtml(): List<CommentEntity>
}
