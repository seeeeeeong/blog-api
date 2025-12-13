package com.blog.api.domain.post.repository

import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.entity.PostStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    fun incrementViewCount(postId: Long)

    @Query(
        value = """
            SELECT * FROM posts p
            WHERE p.status = :#{#status.name()}
            ORDER BY p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM posts p
            WHERE p.status = :#{#status.name()}
        """,
        nativeQuery = true
    )
    fun findByStatus(@Param("status") status: PostStatus, pageable: Pageable): Page<Post>

    @Query(
        value = """
            SELECT * FROM posts p
            WHERE p.category_id = :categoryId
            AND p.status = :#{#status.name()}
            ORDER BY p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM posts p
            WHERE p.category_id = :categoryId
            AND p.status = :#{#status.name()}
        """,
        nativeQuery = true
    )
    fun findByCategoryAndStatus(
        @Param("categoryId") categoryId: Long,
        @Param("status") status: PostStatus,
        pageable: Pageable
    ): Page<Post>

    @Query(
        value = """
            SELECT * FROM posts p
            WHERE p.user_id = :userId
            ORDER BY p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM posts p
            WHERE p.user_id = :userId
        """,
        nativeQuery = true
    )
    fun findAllByUserId(@Param("userId") userId: Long, pageable: Pageable): Page<Post>

    @Query(
        value = """
            SELECT * FROM posts p
            WHERE p.user_id = :userId
            AND p.status = :#{#status.name()}
            ORDER BY p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM posts p
            WHERE p.user_id = :userId
            AND p.status = :#{#status.name()}
        """,
        nativeQuery = true
    )
    fun findByUserIdAndStatus(
        @Param("userId") userId: Long,
        @Param("status") status: PostStatus,
        pageable: Pageable
    ): Page<Post>

    @Query(
        value = """
            SELECT * FROM posts p
            WHERE p.status = 'PUBLISHED'
            ORDER BY p.view_count DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findTopByViewCount(@Param("limit") limit: Int): List<Post>

    @Query(
        value = """
            SELECT * FROM posts p
            WHERE (:keyword IS NULL OR
                   LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:categoryId IS NULL OR p.category_id = :categoryId)
            AND (:status IS NULL OR p.status = :status)
            ORDER BY p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*) FROM posts p
            WHERE (:keyword IS NULL OR
                   LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:categoryId IS NULL OR p.category_id = :categoryId)
            AND (:status IS NULL OR p.status = :status)
        """,
        nativeQuery = true
    )
    fun search(
        @Param("keyword") keyword: String? = null,
        @Param("categoryId") categoryId: Long? = null,
        @Param("status") status: String? = null,
        pageable: Pageable
    ): Page<Post>
}