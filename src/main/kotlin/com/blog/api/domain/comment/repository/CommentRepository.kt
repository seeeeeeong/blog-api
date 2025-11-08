package com.blog.api.domain.comment.repository

import com.blog.api.domain.comment.entity.Comment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {
    
    fun findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(postId: Long): List<Comment>
    
    fun findByParentIdOrderByCreatedAtAsc(parentId: Long): List<Comment>
    
    fun countByPostId(postId: Long): Long
}
