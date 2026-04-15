package com.blog.api.core.domain

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.PostEntity
import com.blog.api.storage.PostRepository
import com.blog.api.storage.toPost
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postMarkdownConverter: PostMarkdownConverter,
    private val cacheManager: CacheManager,
) {
    companion object {
        private const val RECENT_VIEW_TTL_HOURS = 1L
        private const val RECENT_VIEW_MAX_SIZE = 10_000L
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    private val recentViews = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(RECENT_VIEW_TTL_HOURS))
        .maximumSize(RECENT_VIEW_MAX_SIZE)
        .build<String, Boolean>()

    @Transactional
    fun createPost(postCreate: PostCreate): Post {
        val entity = createEntity(postCreate)
        return postRepository.save(entity).toPost()
    }

    fun getPost(postId: Long, clientIp: String, userId: Long? = null, isAdmin: Boolean = false): Post {
        val post = findPostById(postId)
        when (post.status) {
            PostStatus.DELETED -> throw CoreException(ErrorType.POST_NOT_FOUND)
            PostStatus.DRAFT -> {
                if (!canAccessDraft(post, userId, isAdmin)) {
                    throw CoreException(ErrorType.POST_NOT_FOUND)
                }
                return post.toPost()
            }
            PostStatus.PUBLISHED -> {
                incrementViewIfNeeded(postId, clientIp)
                return post.toPost()
            }
        }
    }

    fun getPostForAdmin(postId: Long, userId: Long): Post {
        val post = findPostById(postId)
        if (post.status == PostStatus.DELETED) {
            throw CoreException(ErrorType.POST_NOT_FOUND)
        }
        checkOwnership(post, userId)
        return post.toPost()
    }

    fun getHtml(postId: Long, content: String): String {
        val cache = cacheManager.getCache("post-html")
            ?: return postMarkdownConverter.convertToHtml(content)

        return try {
            cache.get(postId, String::class.java)
                ?: postMarkdownConverter.convertToHtml(content).also {
                    cache.put(postId, it)
                }
        } catch (e: Exception) {
            logger.warn("[Cache] local(caffeine) cache failure - postId={}: {}", postId, e.message)
            postMarkdownConverter.convertToHtml(content)
        }
    }

    @Cacheable("popular-posts", sync = true)
    fun getPopularPosts(limit: Int): List<Post> {
        val pageable = PageRequest.of(0, limit.coerceAtLeast(1), Sort.by(Sort.Direction.DESC, "viewCount"))
        return postRepository.findByStatus(PostStatus.PUBLISHED, pageable).content.map { it.toPost() }
    }

    fun getAllPosts(pageable: Pageable): Slice<Post> {
        return postRepository.findByStatus(PostStatus.PUBLISHED, pageable).map { it.toPost() }
    }

    fun getPostsByCategory(categoryId: Long, pageable: Pageable): Slice<Post> {
        return postRepository.findByCategoryIdAndStatus(categoryId, PostStatus.PUBLISHED, pageable).map { it.toPost() }
    }

    fun getDraftPosts(userId: Long, pageable: Pageable): Slice<Post> {
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

        post.updateContent(
            categoryId = postUpdate.categoryId,
            title = postUpdate.title,
            content = postUpdate.content,
            thumbnailUrl = postUpdate.thumbnailUrl,
            status = postUpdate.status,
        )

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                evictHtmlCache(postId)
                cacheManager.getCache("popular-posts")?.clear()
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
                evictHtmlCache(postId)
                cacheManager.getCache("popular-posts")?.clear()
            }
        })
    }

    private fun incrementViewIfNeeded(postId: Long, clientIp: String) {
        val key = "$postId:${clientIp.trim().ifBlank { "unknown" }}"
        if (recentViews.getIfPresent(key) != null) return
        recentViews.put(key, true)
        postRepository.incrementViewCount(postId)
    }

    private fun evictHtmlCache(postId: Long) {
        try {
            cacheManager.getCache("post-html")?.evict(postId)
        } catch (e: Exception) {
            logger.warn("[Cache] local(caffeine) cache evict failure - postId={}: {}", postId, e.message)
        }
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

    private fun canAccessDraft(post: PostEntity, userId: Long?, isAdmin: Boolean): Boolean {
        return isAdmin && userId != null && post.userId == userId
    }
}
