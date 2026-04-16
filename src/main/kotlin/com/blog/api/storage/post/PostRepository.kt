package com.blog.api.storage.post

import com.blog.api.core.enum.PostStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<PostEntity, Long> {

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

    @Query(
        value = """
            SELECT p.*
            FROM posts p
            WHERE p.status = 'PUBLISHED'
            AND (CAST(:categoryId AS BIGINT) IS NULL OR p.category_id = :categoryId)
            AND (
                p.title ILIKE CONCAT('%', :query, '%')
                OR p.content ILIKE CONCAT('%', :query, '%')
            )
            ORDER BY p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM posts p
            WHERE p.status = 'PUBLISHED'
            AND (CAST(:categoryId AS BIGINT) IS NULL OR p.category_id = :categoryId)
            AND (
                p.title ILIKE CONCAT('%', :query, '%')
                OR p.content ILIKE CONCAT('%', :query, '%')
            )
        """,
        nativeQuery = true,
    )
    fun searchByKeyword(
        @Param("query") query: String,
        @Param("categoryId") categoryId: Long?,
        pageable: Pageable,
    ): Page<PostEntity>
}
