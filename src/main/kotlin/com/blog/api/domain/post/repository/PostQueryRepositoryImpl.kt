package com.blog.api.domain.post.repository

import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.entity.PostStatus
import com.blog.api.domain.post.entity.QPost
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class PostQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PostQueryRepository {

    private val post = QPost.post

    override fun findByStatus(status: PostStatus, pageable: Pageable): Page<Post> {
        val results = queryFactory
            .selectFrom(post)
            .where(post.status.eq(status))
            .orderBy(post.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(post.count())
            .from(post)
            .where(post.status.eq(status))
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }

    override fun findByCategoryIdAndStatus(
        categoryId: Long,
        status: PostStatus,
        pageable: Pageable
    ): Page<Post> {
        val results = queryFactory
            .selectFrom(post)
            .where(
                post.categoryId.eq(categoryId),
                post.status.eq(status)
            )
            .orderBy(post.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(post.count())
            .from(post)
            .where(
                post.categoryId.eq(categoryId),
                post.status.eq(status)
            )
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }

    override fun findByUserId(userId: Long, pageable: Pageable): Page<Post> {
        val results = queryFactory
            .selectFrom(post)
            .where(post.userId.eq(userId))
            .orderBy(post.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(post.count())
            .from(post)
            .where(post.userId.eq(userId))
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }

    override fun searchPosts(
        keyword: String?,
        categoryId: Long?,
        status: PostStatus?,
        pageable: Pageable
    ): Page<Post> {
        val builder = BooleanBuilder()

        keyword?.let {
            builder.and(
                post.title.containsIgnoreCase(it)
                    .or(post.content.containsIgnoreCase(it))
            )
        }

        categoryId?.let {
            builder.and(post.categoryId.eq(it))
        }

        status?.let {
            builder.and(post.status.eq(it))
        }

        val results = queryFactory
            .selectFrom(post)
            .where(builder)
            .orderBy(post.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(post.count())
            .from(post)
            .where(builder)
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }
}
