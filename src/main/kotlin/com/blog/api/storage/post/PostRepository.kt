package com.blog.api.storage.post

import com.blog.api.core.enum.PostStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<PostEntity, Long>, PostRepositoryCustom {

    fun existsByIdAndStatus(id: Long, status: PostStatus): Boolean

    fun findByStatus(status: PostStatus, pageable: Pageable): Slice<PostEntity>

    fun findByCategoryIdAndStatus(
        categoryId: Long,
        status: PostStatus,
        pageable: Pageable,
    ): Slice<PostEntity>

    fun findByUserIdAndStatus(
        userId: Long,
        status: PostStatus,
        pageable: Pageable,
    ): Slice<PostEntity>
}
