package com.blog.api.domain.post.service

import com.blog.api.domain.post.dto.*
import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.entity.PostStatus
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.common.exception.CustomException
import com.blog.api.common.exception.ErrorCode
import com.blog.api.common.redis.service.RedisBaseService
import com.blog.api.common.util.MarkdownUtil
import com.blog.api.infrastructure.openai.service.EmbeddingService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val redisBaseService: RedisBaseService,
    private val markdownUtil: MarkdownUtil,
    private val embeddingService: EmbeddingService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val VIEW_KEY_PREFIX = "post:view:"
        private const val VIEW_EXPIRATION_HOURS = 1L
        private const val VIEW_COUNT_VALUE = "1"
    }

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

        // 벡터 생성 (동기 처리)
        try {
            post.contentVector = embeddingService.createEmbedding(request.content)
            logger.info("Vector created successfully for new post")
        } catch (e: Exception) {
            logger.error("Failed to create embedding for new post. Continuing without vector.", e)
            // 벡터 생성 실패해도 게시글 저장은 진행 (Graceful Degradation)
        }

        val savedPost = postRepository.save(post)
        return PostResponse.from(savedPost, markdownUtil.convertToHtml(savedPost.content))
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, request: UpdatePostRequest): PostResponse {
        val post = findPostByIdAndValidateOwner(postId, userId)

        // content 변경 여부 확인
        val contentChanged = post.content != request.content

        post.categoryId = request.categoryId
        post.title = request.title
        post.content = request.content
        post.thumbnailUrl = request.thumbnailUrl
        post.status = if (request.isDraft) PostStatus.DRAFT else PostStatus.PUBLISHED

        // content가 변경된 경우에만 벡터 재생성
        if (contentChanged) {
            try {
                post.contentVector = embeddingService.createEmbedding(request.content)
                logger.info("Content vector updated for post $postId")
            } catch (e: Exception) {
                logger.error("Failed to update embedding for post $postId. Keeping old vector.", e)
                // 벡터 업데이트 실패해도 나머지 필드는 업데이트
            }
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

        if (post.status == PostStatus.PUBLISHED) increaseViewCount(postId, clientIp)

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

    /**
     * 유사도 기반 검색
     */
    fun searchPostsBySimilarity(
        query: String,
        categoryId: Long? = null,
        pageable: Pageable
    ): PostListResponse {
        // 쿼리를 벡터로 변환
        val queryVector = try {
            embeddingService.createEmbedding(query)
        } catch (e: Exception) {
            logger.error("Failed to create embedding for search query. Falling back to keyword search.", e)
            // 벡터 생성 실패 시 키워드 검색으로 폴백
            return searchPosts(query, categoryId, pageable)
        }

        // 유사도 검색 실행
        val posts = postRepository.searchBySimilarity(
            queryVector = queryVector,
            categoryId = categoryId,
            status = PostStatus.PUBLISHED.name,
            pageable = pageable
        )

        return PostListResponse.from(posts, markdownUtil::convertToHtml)
    }

    /**
     * 특정 게시글과 유사한 게시글 추천
     */
    fun getSimilarPosts(postId: Long, limit: Int = 5): List<PostResponse> {
        validatePostExists(postId)

        val similarPosts = postRepository.findSimilarPosts(postId, limit)
        return similarPosts.map {
            PostResponse.from(it, markdownUtil.convertToHtml(it.content))
        }
    }

    fun validatePostExists(postId: Long) {
        postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }
    }

    private fun findPostByIdAndValidateOwner(postId: Long, userId: Long): Post =
        postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }
            .takeIf { it.userId == userId }
            ?: throw CustomException(ErrorCode.FORBIDDEN)

    private fun increaseViewCount(postId: Long, clientIp: String) {
        val key = "$VIEW_KEY_PREFIX$postId:$clientIp"
        if (redisBaseService.setIfAbsent(key, VIEW_COUNT_VALUE, VIEW_EXPIRATION_HOURS, TimeUnit.HOURS)) {
            postRepository.incrementViewCount(postId)
        }
    }
}
