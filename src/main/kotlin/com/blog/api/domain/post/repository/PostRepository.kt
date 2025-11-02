package com.blog.api.domain.post.repository

import com.blog.api.domain.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long> {
    
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Post>
    
    fun findByCategoryIdOrderByCreatedAtDesc(categoryId: Long, pageable: Pageable): Page<Post>
    
    fun findByUserId(userId: Long, pageable: Pageable): Page<Post>
}
