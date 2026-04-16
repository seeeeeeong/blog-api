package com.blog.api.core.domain.post

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.enum.UserRole
import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.post.PostEntity
import com.blog.api.storage.post.PostRepository
import com.blog.api.storage.post.toPost
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.HtmlUtils

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postMarkdownConverter: PostMarkdownConverter,
    private val cacheManager: CacheManager,
    private val eventPublisher: ApplicationEventPublisher,
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Transactional
    fun createPost(postCreate: PostCreate): Post {
        val entity = createEntity(postCreate)
        return postRepository.save(entity).toPost()
    }

    @Transactional
    fun getPost(command: PostViewCommand): Post {
        val post = findPostById(command.postId)
        return when (post.status) {
            PostStatus.DELETED -> throw CoreException(ErrorType.POST_NOT_FOUND)
            PostStatus.DRAFT -> {
                val accessible = canAccessDraft(post, command)
                if (accessible) return post.toPost()
                throw CoreException(ErrorType.POST_NOT_FOUND)
            }
            PostStatus.PUBLISHED -> {
                if (!command.hasViewedCookie) {
                    eventPublisher.publishEvent(PostViewedEvent(command.postId))
                }
                post.toPost()
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
            ?: return convertToHtmlSafely(postId, content)

        return try {
            cache.get(postId, String::class.java)
                ?: convertAndCache(cache, postId, content)
        } catch (e: Exception) {
            log.warn(e) { "Cache read failed: postId=$postId" }
            convertToHtmlSafely(postId, content)
        }
    }

    private fun convertAndCache(cache: Cache, postId: Long, content: String): String {
        val html = try {
            postMarkdownConverter.convertToHtml(content)
        } catch (e: Exception) {
            log.warn(e) { "Markdown conversion failed: postId=$postId" }
            return HtmlUtils.htmlEscape(content)
        }
        try {
            cache.put(postId, html)
        } catch (e: Exception) {
            log.warn(e) { "Cache write failed: postId=$postId" }
        }
        return html
    }

    private fun convertToHtmlSafely(postId: Long, content: String): String {
        return try {
            postMarkdownConverter.convertToHtml(content)
        } catch (e: Exception) {
            log.warn(e) { "Markdown conversion failed: postId=$postId" }
            HtmlUtils.htmlEscape(content)
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

        eventPublisher.publishEvent(PostCacheEvictEvent(postId))

        return post.toPost()
    }

    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        val post = findPostById(postId)
        checkOwnership(post, userId)
        post.softDelete()
        eventPublisher.publishEvent(PostCacheEvictEvent(postId))
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

    private fun canAccessDraft(post: PostEntity, command: PostViewCommand): Boolean {
        return command.viewerRole == UserRole.ADMIN &&
            command.viewerUserId != null &&
            post.userId == command.viewerUserId
    }
}
