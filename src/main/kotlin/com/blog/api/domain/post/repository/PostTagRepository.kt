package com.blog.api.domain.post.repository

import com.blog.api.domain.post.entity.PostTag
import org.springframework.data.jpa.repository.JpaRepository

interface PostTagRepository : JpaRepository<PostTag, Long> {
    
    fun findByPostId(postId: Long): List<PostTag>
    
    fun deleteByPostId(postId: Long)
}