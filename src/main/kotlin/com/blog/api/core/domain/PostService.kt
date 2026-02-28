package com.blog.api.core.domain

import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.enum.PostStatus
import com.blog.api.storage.PostEntity
import com.blog.api.storage.PostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postMarkdownConverter: PostMarkdownConverter,
    private val postViewService: PostViewService,
) {

    @Transactional
    fun createPost(postCreate: PostCreate): Post {
        val entity = createEntity(postCreate)
        return toPost(postRepository.save(entity))
    }

    @Transactional
    fun getPost(postId: Long, clientIp: String): Post {
        val post = findPostById(postId)
        if (post.status == PostStatus.DELETED) {
            throw CoreException(ErrorType.POST_NOT_FOUND)
        }
        incrementViewCountIfNeeded(postId, clientIp)
        return toPost(post)
    }

    fun getAllPosts(pageable: Pageable): Page<Post> {
        return postRepository.findByStatus(PostStatus.PUBLISHED, pageable).map { toPost(it) }
    }

    fun getPostsByCategory(categoryId: Long, pageable: Pageable): Page<Post> {
        return postRepository.findByCategoryIdAndStatus(categoryId, PostStatus.PUBLISHED, pageable).map { toPost(it) }
    }

    fun getDraftPosts(userId: Long, pageable: Pageable): Page<Post> {
        return postRepository.findByUserIdAndStatus(userId, PostStatus.DRAFT, pageable).map { toPost(it) }
    }

    fun searchPosts(query: String, categoryId: Long?, pageable: Pageable): Page<Post> {
        if (query.isBlank()) {
            return Page.empty(pageable)
        }
        return searchByQuery(query, categoryId, pageable).map { toPost(it) }
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, postUpdate: PostUpdate): Post {
        val post = findPostById(postId)
        checkOwnership(post, userId)

        post.updateContent(
            categoryId = postUpdate.categoryId,
            title = postUpdate.title,
            content = postUpdate.content,
            thumbnailUrl = postUpdate.thumbnailUrl.takeIf { it.isNotBlank() },
            status = postUpdate.status,
        )

        return toPost(post)
    }

    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        val post = findPostById(postId)
        checkOwnership(post, userId)
        post.softDelete()
    }

    private fun findPostById(postId: Long): PostEntity =
        postRepository.findById(postId).orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

    private fun createEntity(postCreate: PostCreate): PostEntity {
        return PostEntity(
            userId = postCreate.userId,
            categoryId = postCreate.categoryId,
            title = postCreate.title,
            content = postCreate.content,
            thumbnailUrl = postCreate.thumbnailUrl.takeIf { it.isNotBlank() },
            status = postCreate.status,
        )
    }

    private fun incrementViewCountIfNeeded(postId: Long, clientIp: String) {
        val shouldIncrement = postViewService.shouldIncrement(postId, clientIp)
        if (shouldIncrement) {
            postRepository.incrementViewCount(postId)
        }
    }

    private fun searchByQuery(query: String, categoryId: Long?, pageable: Pageable): Page<PostEntity> {
        if (categoryId == null) {
            return postRepository.searchByKeyword(query, pageable)
        }
        return postRepository.searchByKeywordWithCategory(query, categoryId, pageable)
    }

    private fun checkOwnership(post: PostEntity, userId: Long) {
        if (post.userId == userId) {
            return
        }
        throw CoreException(ErrorType.FORBIDDEN)
    }

    private fun toPost(entity: PostEntity): Post {
        return Post(
            id = entity.id!!,
            userId = entity.userId,
            categoryId = entity.categoryId,
            title = entity.title,
            content = entity.content,
            contentHtml = postMarkdownConverter.convertToHtml(entity.content),
            thumbnailUrl = entity.thumbnailUrl.orEmpty(),
            viewCount = entity.viewCount,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
