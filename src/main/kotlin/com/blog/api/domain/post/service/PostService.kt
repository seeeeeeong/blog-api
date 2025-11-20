package com.blog.api.domain.post.service

import com.blog.api.domain.post.dto.CreatePostRequest
import com.blog.api.domain.post.dto.PostListResponse
import com.blog.api.domain.post.dto.PostResponse
import com.blog.api.domain.post.dto.UpdatePostRequest
import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.entity.PostStatus
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import org.springframework.data.domain.PageRequest
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

        return PostResponse.from(postRepository.save(post))
    }

    @Transactional
    fun getPost(postId: Long, clientIp: String): PostResponse {
        val post = postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }

        // Published 게시글만 조회수 증가
        when (post.status) {
            PostStatus.PUBLISHED -> {
                val viewKey = "post:view:$postId:$clientIp"
                when (redisTemplate.opsForValue().get(viewKey)) {
                    null -> {
                        postRepository.incrementViewCount(postId)
                        redisTemplate.opsForValue().set(viewKey, "1", 1, TimeUnit.HOURS)
                    }
                }
            }
            PostStatus.DRAFT -> { /* Draft는 조회수 증가 안 함 */ }
        }

        return PostResponse.from(post)
    }

    fun getAllPosts(page: Int, size: Int): PostListResponse {
        val pageable: Pageable = PageRequest.of(page, size)
        // Published 게시글만 조회
        val postPage = postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED, pageable)

        return PostListResponse(
            posts = postPage.content.map { PostResponse.from(it) },
            totalPages = postPage.totalPages,
            totalElements = postPage.totalElements,
            currentPage = postPage.number,
            pageSize = postPage.size
        )
    }

    fun getPostsByCategory(categoryId: Long, page: Int, size: Int): PostListResponse {
        val pageable: Pageable = PageRequest.of(page, size)
        val postPage = postRepository.findByCategoryIdAndStatusOrderByCreatedAtDesc(
            categoryId,
            PostStatus.PUBLISHED,
            pageable
        )

        return PostListResponse(
            posts = postPage.content.map { PostResponse.from(it) },
            totalPages = postPage.totalPages,
            totalElements = postPage.totalElements,
            currentPage = postPage.number,
            pageSize = postPage.size
        )
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, request: UpdatePostRequest): PostResponse {
        val post = postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }

        when (post.userId == userId) {
            false -> throw CustomException(ErrorCode.FORBIDDEN)
            true -> {
                post.categoryId = request.categoryId
                post.title = request.title
                post.content = request.content
                post.thumbnailUrl = request.thumbnailUrl
                post.status = if (request.isDraft) PostStatus.DRAFT else PostStatus.PUBLISHED
            }
        }

        return PostResponse.from(post)
    }

    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        val post = postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }

        when (post.userId == userId) {
            false -> throw CustomException(ErrorCode.FORBIDDEN)
            true -> postRepository.delete(post)
        }
    }
}