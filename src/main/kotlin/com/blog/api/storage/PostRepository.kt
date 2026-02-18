package com.blog.api.storage

import com.blog.api.core.enum.PostStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<PostEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostEntity p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    fun incrementViewCount(@Param("postId") postId: Long)

    fun findByStatus(status: PostStatus, pageable: Pageable): Page<PostEntity>

    fun findByCategoryIdAndStatus(
        categoryId: Long,
        status: PostStatus,
        pageable: Pageable
    ): Page<PostEntity>

    fun findAllByUserId(userId: Long, pageable: Pageable): Page<PostEntity>

    fun findByUserIdAndStatus(
        userId: Long,
        status: PostStatus,
        pageable: Pageable
    ): Page<PostEntity>

    @Query(
        value = """
            SELECT p.*
            FROM posts p
            WHERE p.content_vector IS NOT NULL
            AND p.status = 'PUBLISHED'
            AND (p.content_vector <-> CAST(:queryVector AS vector)) < :maxDistance
            ORDER BY p.content_vector <-> CAST(:queryVector AS vector)
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM posts p
            WHERE p.content_vector IS NOT NULL
            AND p.status = 'PUBLISHED'
            AND (p.content_vector <-> CAST(:queryVector AS vector)) < :maxDistance
        """,
        nativeQuery = true
    )
    fun searchBySimilarity(
        @Param("queryVector") queryVector: String,
        @Param("maxDistance") maxDistance: Double,
        pageable: Pageable
    ): Page<PostEntity>

    @Query(
        value = """
            SELECT p.*
            FROM posts p
            WHERE p.content_vector IS NOT NULL
            AND p.category_id = :categoryId
            AND p.status = 'PUBLISHED'
            AND (p.content_vector <-> CAST(:queryVector AS vector)) < :maxDistance
            ORDER BY p.content_vector <-> CAST(:queryVector AS vector)
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM posts p
            WHERE p.content_vector IS NOT NULL
            AND p.category_id = :categoryId
            AND p.status = 'PUBLISHED'
            AND (p.content_vector <-> CAST(:queryVector AS vector)) < :maxDistance
        """,
        nativeQuery = true
    )
    fun searchBySimilarityWithCategory(
        @Param("queryVector") queryVector: String,
        @Param("maxDistance") maxDistance: Double,
        @Param("categoryId") categoryId: Long,
        pageable: Pageable
    ): Page<PostEntity>

    @Query(
        value = """
            SELECT p.*
            FROM posts p
            WHERE p.status = 'PUBLISHED'
            AND (
                LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            ORDER BY p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM posts p
            WHERE p.status = 'PUBLISHED'
            AND (
                LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))
            )
        """,
        nativeQuery = true
    )
    fun searchByKeyword(
        @Param("query") query: String,
        pageable: Pageable
    ): Page<PostEntity>

    @Query(
        value = """
            SELECT p.*
            FROM posts p
            WHERE p.status = 'PUBLISHED'
            AND p.category_id = :categoryId
            AND (
                LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            ORDER BY p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM posts p
            WHERE p.status = 'PUBLISHED'
            AND p.category_id = :categoryId
            AND (
                LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))
            )
        """,
        nativeQuery = true
    )
    fun searchByKeywordWithCategory(
        @Param("query") query: String,
        @Param("categoryId") categoryId: Long,
        pageable: Pageable
    ): Page<PostEntity>
}
