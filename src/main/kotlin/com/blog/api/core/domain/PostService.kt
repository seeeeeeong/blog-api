package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.enum.PostStatus
import com.blog.api.storage.PostEntity
import com.blog.api.storage.PostRepository
import com.blog.api.storage.toPost
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postHtmlCacheService: PostHtmlCacheService,
    private val postViewService: PostViewService,
    private val postRankingService: PostRankingService,
) {

    @Transactional
    fun createPost(postCreate: PostCreate): Post {
        val entity = createEntity(postCreate)
        return postRepository.save(entity).toPost()
    }

    fun getPost(postId: Long, clientIp: String): Post {
        val post = findPostById(postId)
        if (post.status == PostStatus.DELETED || post.status == PostStatus.DRAFT) {
            throw CoreException(ErrorType.POST_NOT_FOUND)
        }
        postViewService.incrementIfNeeded(postId, clientIp)
        return post.toPost()
    }

    fun getHtml(postId: Long, content: String): String {
        return postHtmlCacheService.getHtml(postId, content)
    }

    fun getPopularPosts(limit: Int): List<Post> {
        val ids = postRankingService.getTopPostIds(limit)
        if (ids.isEmpty()) return emptyList()
        val postMap = postRepository.findAllByIdInAndStatus(ids, PostStatus.PUBLISHED).associateBy { it.id }
        return ids.mapNotNull { postMap[it]?.toPost() }
    }

    fun getAllPosts(pageable: Pageable): Page<Post> {
        return postRepository.findByStatus(PostStatus.PUBLISHED, pageable).map { it.toPost() }
    }

    fun getPostsByCategory(categoryId: Long, pageable: Pageable): Page<Post> {
        return postRepository.findByCategoryIdAndStatus(categoryId, PostStatus.PUBLISHED, pageable).map { it.toPost() }
    }

    fun getDraftPosts(userId: Long, pageable: Pageable): Page<Post> {
        return postRepository.findByUserIdAndStatus(userId, PostStatus.DRAFT, pageable).map { it.toPost() }
    }

    fun searchPosts(query: String, categoryId: Long?, pageable: Pageable): Page<Post> {
        if (query.isBlank()) {
            return Page.empty(pageable)
        }
        return searchByQuery(query, categoryId, pageable).map { it.toPost() }
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, postUpdate: PostUpdate): Post {
        val post = findPostById(postId)
        checkOwnership(post, userId)
        val oldStatus = post.status

        post.updateContent(
            categoryId = postUpdate.categoryId,
            title = postUpdate.title,
            content = postUpdate.content,
            thumbnailUrl = postUpdate.thumbnailUrl,
            status = postUpdate.status,
        )

        val viewCount = post.viewCount
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                when {
                    postUpdate.status != PostStatus.PUBLISHED -> postRankingService.remove(postId)
                    oldStatus != PostStatus.PUBLISHED -> postRankingService.seed(postId, viewCount)
                }
                postHtmlCacheService.evict(postId)
            }
        })

        return post.toPost()
    }

    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        val post = findPostById(postId)
        checkOwnership(post, userId)
        post.softDelete()
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                postRankingService.remove(postId)
                postHtmlCacheService.evict(postId)
            }
        })
    }

    private fun findPostById(postId: Long): PostEntity =
        postRepository.findById(postId).orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

    private fun createEntity(postCreate: PostCreate): PostEntity {
        return PostEntity(
            userId = postCreate.userId,
            categoryId = postCreate.categoryId,
            title = postCreate.title,
            content = postCreate.content,
            thumbnailUrl = postCreate.thumbnailUrl,
            status = postCreate.status,
        )
    }

    private fun searchByQuery(query: String, categoryId: Long?, pageable: Pageable): Page<PostEntity> {
        if (categoryId == null) {
            return postRepository.searchByKeyword(query, pageable)
        }
        return postRepository.searchByKeywordWithCategory(query, categoryId, pageable)
    }

    private fun checkOwnership(post: PostEntity, userId: Long) {
        if (post.userId != userId) throw CoreException(ErrorType.FORBIDDEN)
    }
}
