package com.blog.api.storage

import com.blog.api.core.enum.PostStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<PostEntity, Long> {

    fun existsByIdAndStatus(id: Long, status: PostStatus): Boolean

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostEntity p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    fun incrementViewCount(@Param("postId") postId: Long)

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostEntity p SET p.viewCount = p.viewCount + :delta WHERE p.id = :postId")
    fun incrementViewCountBy(@Param("postId") postId: Long, @Param("delta") delta: Int)

    fun findAllByIdInAndStatus(ids: List<Long>, status: PostStatus): List<PostEntity>

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

    // ILIKE + pg_trgm GIN 인덱스: 부분 문자열 검색 의미를 그대로 유지하면서 인덱스 사용
    // (3자 미만 쿼리는 trigram 후보 없어 seq scan으로 fallback — 개인 블로그 규모에서는 무방)
    @Query(
        value = """
            SELECT p.*
            FROM posts p
            WHERE p.status = 'PUBLISHED'
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
            AND (
                p.title ILIKE CONCAT('%', :query, '%')
                OR p.content ILIKE CONCAT('%', :query, '%')
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
                p.title ILIKE CONCAT('%', :query, '%')
                OR p.content ILIKE CONCAT('%', :query, '%')
            )
            ORDER BY p.created_at DESC
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM posts p
            WHERE p.status = 'PUBLISHED'
            AND p.category_id = :categoryId
            AND (
                p.title ILIKE CONCAT('%', :query, '%')
                OR p.content ILIKE CONCAT('%', :query, '%')
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
