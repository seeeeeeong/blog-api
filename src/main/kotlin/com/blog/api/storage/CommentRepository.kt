package com.blog.api.storage

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<CommentEntity, Long> {

    @Query("SELECT c FROM CommentEntity c WHERE c.postId = :postId AND c.isDeleted = false ORDER BY c.createdAt ASC")
    fun findAllByPostId(@Param("postId") postId: Long): List<CommentEntity>
}
