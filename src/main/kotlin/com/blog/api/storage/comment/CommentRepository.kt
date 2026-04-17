package com.blog.api.storage.comment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<CommentEntity, Long> {

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId ORDER BY c.createdAt DESC")
    fun findByPostId(@Param("postId") postId: Long): List<CommentEntity>
}
