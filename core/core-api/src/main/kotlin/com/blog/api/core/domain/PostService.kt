package com.blog.api.core.domain

import com.blog.api.core.integration.embedding.EmbeddingService
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.util.MarkdownUtil
import com.blog.api.enums.PostStatus
import com.blog.api.storage.db.core.PostEntity
import com.blog.api.storage.db.core.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postViewService: PostViewService,
    private val markdownUtil: MarkdownUtil,
    private val embeddingService: EmbeddingService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createPost(postCreate: PostCreate): Post {
        val entity = PostEntity(
            userId = postCreate.userId,
            categoryId = postCreate.categoryId,
            title = postCreate.title,
            content = postCreate.content,
            thumbnailUrl = postCreate.thumbnailUrl.takeIf { it.isNotBlank() },
            status = postCreate.status
        )

        try {
            val embedding = embeddingService.createEmbedding(postCreate.content)
            entity.updateVector(embedding)
        } catch (e: Exception) {
            logger.error("Failed to generate embedding for post", e)
        }

        val saved = postRepository.save(entity)
        return toPost(saved)
    }

    fun getPost(postId: Long, clientIp: String): Post {
        val post = postRepository.findById(postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

        postViewService.incrementViewCountOnce(postId, clientIp)
        return toPost(post)
    }

    fun getAllPosts(pageable: Pageable): Page<Post> {
        return postRepository
            .findByStatus(PostStatus.PUBLISHED, pageable)
            .map { toPost(it) }
    }

    fun getPostsByCategory(categoryId: Long, pageable: Pageable): Page<Post> {
        return postRepository
            .findByCategoryIdAndStatus(categoryId, PostStatus.PUBLISHED, pageable)
            .map { toPost(it) }
    }

    fun getMyPosts(userId: Long, pageable: Pageable): Page<Post> {
        return postRepository
            .findAllByUserId(userId, pageable)
            .map { toPost(it) }
    }

    fun getDraftPosts(userId: Long, pageable: Pageable): Page<Post> {
        return postRepository
            .findByUserIdAndStatus(userId, PostStatus.DRAFT, pageable)
            .map { toPost(it) }
    }

    fun getPopularPosts(limit: Int): List<Post> {
        return postRepository
            .findTopByViewCount(limit)
            .map { toPost(it) }
    }

    fun searchPostsBySimilarity(query: String, categoryId: Long?, pageable: Pageable): Page<Post> {
        val embedding = try {
            embeddingService.createEmbedding(query)
        } catch (e: Exception) {
            logger.error("Failed to generate embedding for query: $query", e)
            throw CoreException(ErrorType.INTERNAL_SERVER_ERROR)
        }

        val vectorString = embedding.toList().joinToString(",", "[", "]")

        return if (categoryId != null) {
            postRepository.searchBySimilarityWithCategory(vectorString, categoryId, pageable)
        } else {
            postRepository.searchBySimilarity(vectorString, pageable)
        }.map { toPost(it) }
    }

    fun getSimilarPosts(postId: Long, limit: Int): List<Post> {
        postRepository.findById(postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

        return postRepository
            .findSimilarPosts(postId, limit)
            .map { toPost(it) }
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, postUpdate: PostUpdate): Post {
        val post = postRepository.findById(postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

        if (post.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN)
        }

        post.updateContent(
            categoryId = postUpdate.categoryId,
            title = postUpdate.title,
            content = postUpdate.content,
            thumbnailUrl = postUpdate.thumbnailUrl.takeIf { it.isNotBlank() },
            status = postUpdate.status
        )

        try {
            val embedding = embeddingService.createEmbedding(postUpdate.content)
            post.updateVector(embedding)
        } catch (e: Exception) {
            logger.error("Failed to update embedding for post $postId", e)
        }

        return toPost(post)
    }

    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        val post = postRepository.findById(postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

        if (post.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN)
        }

        postRepository.delete(post)
    }

    private fun toPost(entity: PostEntity): Post {
        val contentHtml = markdownUtil.convertToHtml(entity.content)

        return Post(
            id = entity.id!!,
            userId = entity.userId,
            categoryId = entity.categoryId,
            title = entity.title,
            content = entity.content,
            contentHtml = contentHtml,
            thumbnailUrl = entity.thumbnailUrl ?: "",
            viewCount = entity.viewCount,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
