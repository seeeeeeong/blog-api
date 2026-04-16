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
    fun createPost(newPost: PostCreate): Post {
        val entity = createEntity(newPost)
        return postRepository.save(entity).toPost()
    }

    @Transactional
    fun getPost(viewCommand: PostViewCommand): Post {
        val post = getPostById(viewCommand.postId)
        return when (post.status) {
            PostStatus.DELETED -> throw CoreException(ErrorType.POST_NOT_FOUND)
            PostStatus.DRAFT -> handleDraftAccess(post, viewCommand)
            PostStatus.PUBLISHED -> handlePublishedAccess(post, viewCommand)
        }
    }

    fun getPostForAdmin(postId: Long, userId: Long): Post {
        val post = getPostById(postId)
        if (post.status == PostStatus.DELETED) {
            throw CoreException(ErrorType.POST_NOT_FOUND)
        }
        requireOwner(post, userId)
        return post.toPost()
    }

    fun renderHtml(postId: Long, content: String): String {
        val cache = cacheManager.getCache("post-html")
            ?: return renderMarkdownOrEscape(postId, content)

        return try {
            cache.get(postId, String::class.java)
                ?: cacheAndReturn(cache, postId, content)
        } catch (e: Exception) {
            log.warn(e) { "Cache read failed: postId=$postId" }
            renderMarkdownOrEscape(postId, content)
        }
    }

    private fun cacheAndReturn(cache: Cache, postId: Long, content: String): String {
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

    private fun renderMarkdownOrEscape(postId: Long, content: String): String {
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
        return postRepository.searchByKeyword(query, categoryId, pageable).map { it.toPost() }
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, postUpdate: PostUpdate): Post {
        val post = getPostById(postId)
        requireOwner(post, userId)

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
        val post = getPostById(postId)
        requireOwner(post, userId)
        post.softDelete()
        eventPublisher.publishEvent(PostCacheEvictEvent(postId))
    }

    private fun getPostById(postId: Long): PostEntity =
        postRepository.findById(postId).orElseThrow { CoreException(ErrorType.POST_NOT_FOUND) }

    private fun createEntity(newPost: PostCreate): PostEntity {
        return PostEntity(
            userId = newPost.userId,
            categoryId = newPost.categoryId,
            title = newPost.title,
            content = newPost.content,
            thumbnailUrl = newPost.thumbnailUrl,
            status = newPost.status,
        )
    }

    private fun requireOwner(post: PostEntity, userId: Long) {
        if (post.userId != userId) throw CoreException(ErrorType.FORBIDDEN)
    }

    private fun isDraftAuthor(post: PostEntity, viewCommand: PostViewCommand): Boolean {
        return viewCommand.viewerRole == UserRole.ADMIN &&
            viewCommand.viewerUserId != null &&
            post.userId == viewCommand.viewerUserId
    }

    private fun handleDraftAccess(post: PostEntity, viewCommand: PostViewCommand): Post {
        if (isDraftAuthor(post, viewCommand)) return post.toPost()
        throw CoreException(ErrorType.POST_NOT_FOUND)
    }

    private fun handlePublishedAccess(post: PostEntity, viewCommand: PostViewCommand): Post {
        if (!viewCommand.hasViewedCookie) {
            eventPublisher.publishEvent(PostViewedEvent(viewCommand.postId))
        }
        return post.toPost()
    }
}
