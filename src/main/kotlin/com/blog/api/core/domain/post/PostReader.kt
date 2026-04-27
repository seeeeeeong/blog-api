package com.blog.api.core.domain.post

import com.blog.api.core.api.config.CacheConfig
import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.converter.MarkdownRenderer
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.post.PostRepository
import com.blog.api.storage.post.toPost
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostReader(
    private val postRepository: PostRepository,
    private val markdownRenderer: MarkdownRenderer,
    private val cacheManager: CacheManager,
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun getPost(
        postId: Long,
        userId: Long? = null,
        isAdmin: Boolean = false,
    ): Post {
        val post = findPostById(postId)
        return when (post.status) {
            PostStatus.DELETED -> {
                throw CoreException(ErrorType.POST_NOT_FOUND)
            }

            PostStatus.DRAFT -> {
                val isOwner = isAdmin && userId == post.userId
                if (isOwner) post else throw CoreException(ErrorType.POST_NOT_FOUND)
            }

            PostStatus.PUBLISHED -> {
                post
            }
        }
    }

    fun getPostForAdmin(
        postId: Long,
        userId: Long,
    ): Post {
        val post = findPostById(postId)
        if (post.status == PostStatus.DELETED) throw CoreException(ErrorType.POST_NOT_FOUND)
        if (post.userId == userId) return post
        throw CoreException(ErrorType.FORBIDDEN)
    }

    fun getAllPosts(pageable: Pageable): Slice<Post> =
        postRepository.findByStatus(PostStatus.PUBLISHED, pageable).map { it.toPost() }

    fun getPostsByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Slice<Post> =
        postRepository.findByCategoryIdAndStatus(categoryId, PostStatus.PUBLISHED, pageable).map { it.toPost() }

    fun getDraftPosts(
        userId: Long,
        pageable: Pageable,
    ): Slice<Post> = postRepository.findByUserIdAndStatus(userId, PostStatus.DRAFT, pageable).map { it.toPost() }

    fun searchPosts(
        query: String,
        categoryId: Long?,
        pageable: Pageable,
    ): Page<Post> {
        if (query.isBlank()) return Page.empty(pageable)
        return postRepository.searchByKeyword(query, categoryId, pageable).map { it.toPost() }
    }

    fun renderHtml(
        postId: Long,
        content: String,
    ): String {
        val cache =
            cacheManager.getCache(CacheConfig.POST_HTML)
                ?: return markdownRenderer.renderOrEscape(content, postId)

        readCachedHtml(cache, postId)?.let { return it }

        val html = markdownRenderer.renderOrEscape(content, postId)
        writeCachedHtml(cache, postId, html)
        return html
    }

    private fun readCachedHtml(
        cache: Cache,
        postId: Long,
    ): String? =
        try {
            cache.get(postId, String::class.java)
        } catch (e: Exception) {
            log.warn(e) { "Cache read failed: postId=$postId" }
            null
        }

    private fun writeCachedHtml(
        cache: Cache,
        postId: Long,
        html: String,
    ) {
        try {
            cache.put(postId, html)
        } catch (e: Exception) {
            log.warn(e) { "Cache write failed: postId=$postId" }
        }
    }

    private fun findPostById(postId: Long): Post =
        postRepository.findByIdOrNull(postId)?.toPost()
            ?: throw CoreException(ErrorType.POST_NOT_FOUND)
}
