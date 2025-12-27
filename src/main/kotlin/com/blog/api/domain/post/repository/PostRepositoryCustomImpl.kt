package com.blog.api.domain.post.repository

import com.blog.api.domain.post.entity.Post
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 벡터 유사도 검색을 위한 Custom Repository 구현
 */
@Repository
class PostRepositoryCustomImpl(
    private val entityManager: EntityManager
) : PostRepositoryCustom {

    override fun searchBySimilarity(
        queryVector: FloatArray,
        categoryId: Long?,
        status: String?,
        pageable: Pageable
    ): Page<Post> {

        // 벡터를 pgvector 형식으로 변환 [0.1, 0.2, ...]
        val vectorStr = queryVector.joinToString(",", "[", "]")

        // 데이터 조회 쿼리
        val query = entityManager.createNativeQuery(
            """
            SELECT p.*
            FROM posts p
            WHERE p.content_vector IS NOT NULL
            AND (:categoryId IS NULL OR p.category_id = :categoryId)
            AND (:status IS NULL OR p.status = :status)
            ORDER BY p.content_vector <=> CAST(:queryVector AS vector)
            LIMIT :limit OFFSET :offset
            """,
            Post::class.java
        )

        query.setParameter("queryVector", vectorStr)
        query.setParameter("categoryId", categoryId)
        query.setParameter("status", status)
        query.setParameter("limit", pageable.pageSize)
        query.setParameter("offset", pageable.offset)

        @Suppress("UNCHECKED_CAST")
        val results = query.resultList as List<Post>

        // Count 쿼리
        val countQuery = entityManager.createNativeQuery(
            """
            SELECT COUNT(*)
            FROM posts p
            WHERE p.content_vector IS NOT NULL
            AND (:categoryId IS NULL OR p.category_id = :categoryId)
            AND (:status IS NULL OR p.status = :status)
            """
        )
        countQuery.setParameter("categoryId", categoryId)
        countQuery.setParameter("status", status)

        val total = (countQuery.singleResult as Number).toLong()

        return PageImpl(results, pageable, total)
    }
}
