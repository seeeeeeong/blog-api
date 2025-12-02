package com.blog.api.domain.post.repository

import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.entity.PostStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostQueryRepository {

    fun findByStatus(status: PostStatus, pageable: Pageable): Page<Post>

    fun findByCategoryIdAndStatus(categoryId: Long, status: PostStatus, pageable: Pageable): Page<Post>

    fun findByUserId(userId: Long, pageable: Pageable): Page<Post>

    fun searchPosts(
        keyword: String? = null,
        categoryId: Long? = null,
        status: PostStatus? = null,
        pageable: Pageable
    ): Page<Post>
}
