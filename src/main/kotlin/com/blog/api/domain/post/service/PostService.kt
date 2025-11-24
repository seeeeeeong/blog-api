package com.blog.api.domain.post.service

import com.blog.api.domain.post.dto.*
import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.entity.PostStatus
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
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
    private val redisTemplate: RedisTemplate<String, String>
) {

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

        return PostResponse.from(savedPost)
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

        return PostResponse.from(post)
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

        return PostResponse.from(post)
    }

    fun getAllPosts(pageable: Pageable): PostListResponse {
        val posts = postRepository.findByStatus(PostStatus.PUBLISHED, pageable)
        return PostListResponse.from(posts)
    }

    fun getPostsByCategory(categoryId: Long, pageable: Pageable): PostListResponse {
        val posts = postRepository.findByCategoryIdAndStatus(categoryId, PostStatus.PUBLISHED, pageable)
        return PostListResponse.from(posts)
    }

    fun getMyPosts(userId: Long, pageable: Pageable): PostListResponse {
        val posts = postRepository.findByUserId(userId, pageable)
        return PostListResponse.from(posts)
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
        val viewKey = "post:view:$postId:$clientIp"

        val isFirstView = redisTemplate.opsForValue()
            .setIfAbsent(viewKey, "1", 1, TimeUnit.HOURS) == true

        if (isFirstView) {
            postRepository.incrementViewCount(postId)
        }
    }
}