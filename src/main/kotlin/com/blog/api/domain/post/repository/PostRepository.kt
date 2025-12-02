package com.blog.api.domain.post.repository

import com.blog.api.domain.post.entity.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

/**
 * Command Repository (CQRS - Write)
 * 쓰기 작업(생성, 수정, 삭제)을 담당
 */
interface PostRepository : JpaRepository<Post, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    fun incrementViewCount(postId: Long)
}