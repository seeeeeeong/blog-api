package com.blog.api.domain.post.service

import com.blog.api.domain.post.dto.*
import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.entity.PostStatus
import com.blog.api.domain.post.entity.PostTag
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.domain.post.repository.PostTagRepository
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postTagRepository: PostTagRepository,
    private val redisTemplate: RedisTemplate<String, String>
) {

    @Transactional
    fun createPost(userId: Long, request: CreatePostRequest): PostResponse {
        val post = postRepository.save(
            Post(
                userId = userId,
                categoryId = request.categoryId,
                title = request.title,
                content = request.content,
                thumbnailUrl = request.thumbnailUrl,
                status = if (request.isDraft) PostStatus.DRAFT else PostStatus.PUBLISHED
            )
        )

        if (request.tagIds.isNotEmpty()) {
            val tags = request.tagIds.map { PostTag(postId = post.id!!, tagId = it) }
            postTagRepository.saveAll(tags)
        }

        return PostResponse.from(post, request.tagIds)
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, request: UpdatePostRequest): PostResponse {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw CustomException(ErrorCode.POST_NOT_FOUND)

        if (post.userId != userId) throw CustomException(ErrorCode.FORBIDDEN)

        post.apply {
            categoryId = request.categoryId
            title = request.title
            content = request.content
            thumbnailUrl = request.thumbnailUrl
            status = if (request.isDraft) PostStatus.DRAFT else PostStatus.PUBLISHED
        }

        if (request.tagIds.isNotEmpty()) {
            postTagRepository.deleteByPostId(postId)

            val newTags = request.tagIds.map { PostTag(postId = postId, tagId = it) }
            postTagRepository.saveAll(newTags)
        }

        return PostResponse.from(post, request.tagIds)
    }

    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw CustomException(ErrorCode.POST_NOT_FOUND)

        if (post.userId != userId) throw CustomException(ErrorCode.FORBIDDEN)

        postTagRepository.deleteByPostId(postId)
        postRepository.delete(post)
    }

    @Transactional
    fun getPost(postId: Long, clientIp: String): PostResponse {
        val post = postRepository.findByIdOrNull(postId)
            ?: throw CustomException(ErrorCode.POST_NOT_FOUND)

        if (post.status == PostStatus.PUBLISHED) {
            val viewKey = "post:view:$postId:$clientIp"
            val isFirstView = redisTemplate.opsForValue()
                .setIfAbsent(viewKey, "1", 1, TimeUnit.HOURS) == true

            if (isFirstView) {
                postRepository.incrementViewCount(postId)
            }
        }

        val tags = postTagRepository.findByPostId(postId).map { it.tagId }

        return PostResponse.from(post, tags)
    }

    fun getAllPosts(pageable: Pageable): PostListResponse {
        val posts = postRepository.findByStatus(PostStatus.PUBLISHED, pageable)

        val postIds = posts.content.mapNotNull { it.id }
        val tagsMap = postTagRepository.findByPostIdIn(postIds)
            .groupBy({ it.postId }, { it.tagId })

        return PostListResponse.from(posts, tagsMap)
    }

    fun getPostsByCategory(categoryId: Long, pageable: Pageable): PostListResponse {
        val posts = postRepository.findByCategoryIdAndStatus(categoryId, PostStatus.PUBLISHED, pageable)

        val postIds = posts.content.mapNotNull { it.id }
        val tagsMap = postTagRepository.findByPostIdIn(postIds)
            .groupBy({ it.postId }, { it.tagId })

        return PostListResponse.from(posts, tagsMap)
    }

    fun getMyPosts(userId: Long, pageable: Pageable): PostListResponse {
        val posts = postRepository.findByUserId(userId, pageable)

        val postIds = posts.content.mapNotNull { it.id }
        val tagsMap = postTagRepository.findByPostIdIn(postIds)
            .groupBy({ it.postId }, { it.tagId })

        return PostListResponse.from(posts, tagsMap)
    }
}