package com.blog.api.domain.post.service

import com.blog.api.domain.post.dto.*
import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.entity.PostStatus
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.common.exception.CustomException
import com.blog.api.common.exception.ErrorCode
import com.blog.api.common.util.MarkdownUtil
import com.blog.api.common.redis.ViewCountRedisService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val viewCountRedisService: ViewCountRedisService,
    private val markdownUtil: MarkdownUtil
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
        val posts = postRepository.findByStatus(PostStatus.PUBLISHED, pageable)
        return PostListResponse.from(posts, markdownUtil::convertToHtml)
    }

    fun getPostsByCategory(categoryId: Long, pageable: Pageable): PostListResponse {
        val posts = postRepository.findByCategoryAndStatus(categoryId, PostStatus.PUBLISHED, pageable)
        return PostListResponse.from(posts, markdownUtil::convertToHtml)
    }

    fun searchPosts(keyword: String?, categoryId: Long?, pageable: Pageable): PostListResponse {
        val posts = postRepository.search(
            keyword = keyword,
            categoryId = categoryId,
            status = PostStatus.PUBLISHED.name,
            pageable = pageable
        )
        return PostListResponse.from(posts, markdownUtil::convertToHtml)
    }

    fun getMyPosts(userId: Long, pageable: Pageable): PostListResponse {
        val posts = postRepository.findAllByUserId(userId, pageable)
        return PostListResponse.from(posts, markdownUtil::convertToHtml)
    }

    fun getDraftPosts(userId: Long, pageable: Pageable): PostListResponse {
        val posts = postRepository.findByUserIdAndStatus(userId, PostStatus.DRAFT, pageable)
        return PostListResponse.from(posts, markdownUtil::convertToHtml)
    }

    fun getPopularPosts(limit: Int = 10): List<PostResponse> {
        val posts = postRepository.findTopByViewCount(limit)
        return posts.map { PostResponse.from(it, markdownUtil.convertToHtml(it.content)) }
    }

    fun validatePostExists(postId: Long) {
        postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }
    }

    private fun findPostByIdAndValidateOwner(postId: Long, userId: Long): Post {
        val post = postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }
        if (post.userId != userId) throw CustomException(ErrorCode.FORBIDDEN)
        return post
    }

    private fun increaseViewCount(postId: Long, clientIp: String) {
        if (viewCountRedisService.isFirstView(postId, clientIp)) {
            postRepository.incrementViewCount(postId)
        }
    }
}