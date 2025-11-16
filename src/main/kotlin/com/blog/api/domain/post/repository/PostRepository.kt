package com.blog.api.domain.post.repository

import com.blog.api.domain.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PostRepository : JpaRepository<Post, Long> {
    
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Post>
    
    fun findByCategoryIdOrderByCreatedAtDesc(categoryId: Long, pageable: Pageable): Page<Post>
    
    fun findByUserId(userId: Long, pageable: Pageable): Page<Post>

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    fun incrementViewCount(postId: Long)
}
