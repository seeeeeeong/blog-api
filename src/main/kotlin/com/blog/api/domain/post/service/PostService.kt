package com.blog.api.domain.post.service

import com.blog.api.domain.post.dto.*
import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.entity.PostStatus
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import com.blog.api.global.util.MarkdownUtil
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postQueryRepository: com.blog.api.domain.post.repository.PostQueryRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val markdownUtil: MarkdownUtil
) {
    private val logger = LoggerFactory.getLogger(PostService::class.java)

    @Transactional
    fun createPost(userId: Long, request: CreatePostRequest): PostResponse {
        val post = Post(
            userId = userId,
            categoryId = request.categoryId,
            title = request.title,
            content = request.content,
            thumbnailUrl = request.thumbnailUrl,
            status = if (request.isDraft) PostStatus.DRAFT else PostStatus.PUBLISHED
        )
        val savedPost = postRepository.save(post)

        return PostResponse.from(savedPost, markdownUtil.convertToHtml(savedPost.content))
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, request: UpdatePostRequest): PostResponse {
        val post = findPostByIdAndValidateOwner(postId, userId)

        post.apply {
            categoryId = request.categoryId
            title = request.title
            content = request.content
            thumbnailUrl = request.thumbnailUrl
            status = if (request.isDraft) PostStatus.DRAFT else PostStatus.PUBLISHED
        }

        return PostResponse.from(post, markdownUtil.convertToHtml(post.content))
    }

    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        val post = findPostByIdAndValidateOwner(postId, userId)

        postRepository.delete(post)
    }

    @Transactional
    fun getPost(postId: Long, clientIp: String): PostResponse {
        val post = postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }

        if (post.status == PostStatus.PUBLISHED) {
            increaseViewCount(postId, clientIp)
        }

        return PostResponse.from(post, markdownUtil.convertToHtml(post.content))
    }

    fun getAllPosts(pageable: Pageable): PostListResponse {
        val posts = postQueryRepository.findByStatus(PostStatus.PUBLISHED, pageable)
        return PostListResponse.from(posts, markdownUtil::convertToHtml)
    }

    fun getPostsByCategory(categoryId: Long, pageable: Pageable): PostListResponse {
        val posts = postQueryRepository.findByCategoryIdAndStatus(categoryId, PostStatus.PUBLISHED, pageable)
        return PostListResponse.from(posts, markdownUtil::convertToHtml)
    }

    fun getMyPosts(userId: Long, pageable: Pageable): PostListResponse {
        val posts = postQueryRepository.findByUserId(userId, pageable)
        return PostListResponse.from(posts, markdownUtil::convertToHtml)
    }

    fun validatePostExists(postId: Long) {
        val exists = postRepository.existsById(postId)
        if (exists) return
        throw CustomException(ErrorCode.POST_NOT_FOUND)
    }

    private fun findPostByIdAndValidateOwner(postId: Long, userId: Long): Post {
        val post = postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }
        if (post.userId != userId) throw CustomException(ErrorCode.FORBIDDEN)
        return post
    }

    private fun increaseViewCount(postId: Long, clientIp: String) {
        try {
            val viewKey = "post:view:$postId:$clientIp"

            val isFirstView = redisTemplate.opsForValue()
                .setIfAbsent(viewKey, "1", 1, TimeUnit.HOURS) == true

            if (isFirstView) {
                postRepository.incrementViewCount(postId)
                logger.debug("incrementViewCount Success : postId={}, clientIp={}", postId, clientIp)
            }
        } catch (e: Exception) {
            logger.warn("incrementViewCount Fail : postId={}, clientIp={}, error={}",
                postId, clientIp, e.message, e)
        }
    }
}