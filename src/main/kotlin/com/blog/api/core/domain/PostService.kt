package com.blog.api.core.domain

import com.blog.api.core.support.connector.EmbeddingFacade
import com.blog.api.core.support.connector.RedisOps
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.enum.PostStatus
import com.blog.api.storage.PostEntity
import com.blog.api.storage.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postMarkdownConverter: PostMarkdownConverter,
    private val embeddingFacade: EmbeddingFacade,
    private val redisOps: RedisOps,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val VIEW_KEY_PREFIX = "post:view:"
        private const val VIEW_EXPIRATION_HOURS = 24L
        private const val VIEW_COUNT_VALUE = "1"
    }

    @Transactional
    fun createPost(postCreate: PostCreate): Post {
        val thumbnailUrl = postCreate.thumbnailUrl.takeIf { it.isNotBlank() }
        val entity = PostEntity(
            userId = postCreate.userId,
            categoryId = postCreate.categoryId,
            title = postCreate.title,
            content = postCreate.content,
            thumbnailUrl = thumbnailUrl,
            status = postCreate.status,
        )

        try {
            val embedding = embeddingFacade.createEmbedding(postCreate.content)
            entity.updateVector(embedding)
        } catch (e: Exception) {
            logger.error("Failed to generate embedding for post", e)
        }

        val saved = postRepository.save(entity)
        val contentHtml = postMarkdownConverter.convertToHtml(saved.content)
        val savedThumbnailUrl = saved.thumbnailUrl.orEmpty()

        return Post(
            id = saved.id!!,
            userId = saved.userId,
            categoryId = saved.categoryId,
            title = saved.title,
            content = saved.content,
            contentHtml = contentHtml,
            thumbnailUrl = savedThumbnailUrl,
            viewCount = saved.viewCount,
            status = saved.status,
            createdAt = saved.createdAt,
            updatedAt = saved.updatedAt,
        )
    }

    @Transactional
    fun getPost(postId: Long, clientFingerprint: String): Post {
        val post = postRepository.findById(postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

        val viewKey = "$VIEW_KEY_PREFIX$postId:$clientFingerprint"
        val setResult = redisOps.setIfAbsent(viewKey, VIEW_COUNT_VALUE, VIEW_EXPIRATION_HOURS, TimeUnit.HOURS)
        val shouldIncrement = setResult.isSet || setResult.isError
        if (shouldIncrement) {
            postRepository.incrementViewCount(postId)
        }

        val contentHtml = postMarkdownConverter.convertToHtml(post.content)
        val postThumbnailUrl = post.thumbnailUrl.orEmpty()

        return Post(
            id = post.id!!,
            userId = post.userId,
            categoryId = post.categoryId,
            title = post.title,
            content = post.content,
            contentHtml = contentHtml,
            thumbnailUrl = postThumbnailUrl,
            viewCount = post.viewCount,
            status = post.status,
            createdAt = post.createdAt,
            updatedAt = post.updatedAt,
        )
    }

    fun getAllPosts(pageable: Pageable): Page<Post> {
        return postRepository
            .findByStatus(PostStatus.PUBLISHED, pageable)
            .map { entity ->
                val contentHtml = postMarkdownConverter.convertToHtml(entity.content)
                val thumbnailUrl = entity.thumbnailUrl.orEmpty()

                Post(
                    id = entity.id!!,
                    userId = entity.userId,
                    categoryId = entity.categoryId,
                    title = entity.title,
                    content = entity.content,
                    contentHtml = contentHtml,
                    thumbnailUrl = thumbnailUrl,
                    viewCount = entity.viewCount,
                    status = entity.status,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
    }

    fun getPostsByCategory(categoryId: Long, pageable: Pageable): Page<Post> {
        return postRepository
            .findByCategoryIdAndStatus(categoryId, PostStatus.PUBLISHED, pageable)
            .map { entity ->
                val contentHtml = postMarkdownConverter.convertToHtml(entity.content)
                val thumbnailUrl = entity.thumbnailUrl.orEmpty()

                Post(
                    id = entity.id!!,
                    userId = entity.userId,
                    categoryId = entity.categoryId,
                    title = entity.title,
                    content = entity.content,
                    contentHtml = contentHtml,
                    thumbnailUrl = thumbnailUrl,
                    viewCount = entity.viewCount,
                    status = entity.status,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
    }

    fun getMyPosts(userId: Long, pageable: Pageable): Page<Post> {
        return postRepository
            .findAllByUserId(userId, pageable)
            .map { entity ->
                val contentHtml = postMarkdownConverter.convertToHtml(entity.content)
                val thumbnailUrl = entity.thumbnailUrl.orEmpty()

                Post(
                    id = entity.id!!,
                    userId = entity.userId,
                    categoryId = entity.categoryId,
                    title = entity.title,
                    content = entity.content,
                    contentHtml = contentHtml,
                    thumbnailUrl = thumbnailUrl,
                    viewCount = entity.viewCount,
                    status = entity.status,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
    }

    fun getDraftPosts(userId: Long, pageable: Pageable): Page<Post> {
        return postRepository
            .findByUserIdAndStatus(userId, PostStatus.DRAFT, pageable)
            .map { entity ->
                val contentHtml = postMarkdownConverter.convertToHtml(entity.content)
                val thumbnailUrl = entity.thumbnailUrl.orEmpty()

                Post(
                    id = entity.id!!,
                    userId = entity.userId,
                    categoryId = entity.categoryId,
                    title = entity.title,
                    content = entity.content,
                    contentHtml = contentHtml,
                    thumbnailUrl = thumbnailUrl,
                    viewCount = entity.viewCount,
                    status = entity.status,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
    }

    fun getPopularPosts(limit: Int): List<Post> {
        return postRepository
            .findTopByViewCount(limit)
            .map { entity ->
                val contentHtml = postMarkdownConverter.convertToHtml(entity.content)
                val thumbnailUrl = entity.thumbnailUrl.orEmpty()

                Post(
                    id = entity.id!!,
                    userId = entity.userId,
                    categoryId = entity.categoryId,
                    title = entity.title,
                    content = entity.content,
                    contentHtml = contentHtml,
                    thumbnailUrl = thumbnailUrl,
                    viewCount = entity.viewCount,
                    status = entity.status,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
    }

    fun searchPostsBySimilarity(query: String, categoryId: Long?, pageable: Pageable): Page<Post> {
        val embedding = try {
            embeddingFacade.createEmbedding(query)
        } catch (e: Exception) {
            logger.error("Failed to generate embedding for query: $query", e)
            throw CoreException(ErrorType.INTERNAL_SERVER_ERROR)
        }

        val vectorString = embedding.joinToString(",", "[", "]")

        val results = if (categoryId == null) {
            postRepository.searchBySimilarity(vectorString, pageable)
        } else {
            postRepository.searchBySimilarityWithCategory(vectorString, categoryId, pageable)
        }

        return results.map { entity ->
            val contentHtml = postMarkdownConverter.convertToHtml(entity.content)
            val thumbnailUrl = entity.thumbnailUrl.orEmpty()

            Post(
                id = entity.id!!,
                userId = entity.userId,
                categoryId = entity.categoryId,
                title = entity.title,
                content = entity.content,
                contentHtml = contentHtml,
                thumbnailUrl = thumbnailUrl,
                viewCount = entity.viewCount,
                status = entity.status,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
        }
    }

    fun getSimilarPosts(postId: Long, limit: Int): List<Post> {
        postRepository.findById(postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

        return postRepository
            .findSimilarPosts(postId, limit)
            .map { entity ->
                val contentHtml = postMarkdownConverter.convertToHtml(entity.content)
                val thumbnailUrl = entity.thumbnailUrl.orEmpty()

                Post(
                    id = entity.id!!,
                    userId = entity.userId,
                    categoryId = entity.categoryId,
                    title = entity.title,
                    content = entity.content,
                    contentHtml = contentHtml,
                    thumbnailUrl = thumbnailUrl,
                    viewCount = entity.viewCount,
                    status = entity.status,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, postUpdate: PostUpdate): Post {
        val post = postRepository.findById(postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

        if (post.userId == userId) {
            val thumbnailUrl = postUpdate.thumbnailUrl.takeIf { it.isNotBlank() }
            post.updateContent(
                categoryId = postUpdate.categoryId,
                title = postUpdate.title,
                content = postUpdate.content,
                thumbnailUrl = thumbnailUrl,
                status = postUpdate.status,
            )

            try {
                val embedding = embeddingFacade.createEmbedding(postUpdate.content)
                post.updateVector(embedding)
            } catch (e: Exception) {
                logger.error("Failed to update embedding for post $postId", e)
            }

            val contentHtml = postMarkdownConverter.convertToHtml(post.content)
            val postThumbnailUrl = post.thumbnailUrl.orEmpty()

            return Post(
                id = post.id!!,
                userId = post.userId,
                categoryId = post.categoryId,
                title = post.title,
                content = post.content,
                contentHtml = contentHtml,
                thumbnailUrl = postThumbnailUrl,
                viewCount = post.viewCount,
                status = post.status,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
            )
        }

        throw CoreException(ErrorType.FORBIDDEN)
    }

    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        val post = postRepository.findById(postId)
            .orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

        if (post.userId == userId) {
            postRepository.delete(post)
            return
        }

        throw CoreException(ErrorType.FORBIDDEN)
    }

}
