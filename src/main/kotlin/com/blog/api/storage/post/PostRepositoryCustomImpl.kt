package com.blog.api.storage.post

import com.blog.api.core.enum.PostStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class PostRepositoryCustomImpl(
    @PersistenceContext private val entityManager: EntityManager,
) : PostRepositoryCustom {
    override fun searchByKeyword(
        query: String,
        categoryId: Long?,
        pageable: Pageable,
    ): Page<PostEntity> {
        val lowerQuery = "%${query.lowercase()}%"

        val jpql =
            buildString {
                append("SELECT p FROM PostEntity p")
                append(" WHERE p.status = :status")
                if (categoryId != null) append(" AND p.categoryId = :categoryId")
                append(" AND (LOWER(p.title) LIKE :query OR LOWER(p.content) LIKE :query)")
                append(" ORDER BY p.createdAt DESC")
            }

        val countJpql =
            buildString {
                append("SELECT COUNT(p) FROM PostEntity p")
                append(" WHERE p.status = :status")
                if (categoryId != null) append(" AND p.categoryId = :categoryId")
                append(" AND (LOWER(p.title) LIKE :query OR LOWER(p.content) LIKE :query)")
            }

        val dataQuery =
            entityManager
                .createQuery(jpql, PostEntity::class.java)
                .setParameter("status", PostStatus.PUBLISHED)
                .setParameter("query", lowerQuery)
                .setFirstResult(pageable.offset.toInt())
                .setMaxResults(pageable.pageSize)

        val countQuery =
            entityManager
                .createQuery(countJpql, Long::class.javaObjectType)
                .setParameter("status", PostStatus.PUBLISHED)
                .setParameter("query", lowerQuery)

        if (categoryId != null) {
            dataQuery.setParameter("categoryId", categoryId)
            countQuery.setParameter("categoryId", categoryId)
        }

        val content = dataQuery.resultList
        val total = countQuery.singleResult

        return PageImpl(content, pageable, total)
    }
}
