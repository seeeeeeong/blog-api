package com.blog.api.domain.post.service

import com.blog.api.domain.post.dto.CreatePostRequest
import com.blog.api.domain.post.dto.PostListResponse
import com.blog.api.domain.post.dto.PostResponse
import com.blog.api.domain.post.dto.UpdatePostRequest
import com.blog.api.domain.post.entity.Post
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository
) {
    
    @Transactional
    fun createPost(userId: Long, request: CreatePostRequest): PostResponse {
        val post = Post(
            userId = userId,
            categoryId = request.categoryId,
            title = request.title,
            content = request.content,
            thumbnailUrl = request.thumbnailUrl
        )
        
        return postRepository.save(post).let { PostResponse.from(it) }
    }
    
    fun getPost(postId: Long): PostResponse {
        return postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }
            .let { PostResponse.from(it) }
    }
    
    fun getAllPosts(page: Int, size: Int): PostListResponse {
        val pageable: Pageable = PageRequest.of(page, size)
        val postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable)
        
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
        val postPage = postRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId, pageable)
        
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
        
        (post.userId == userId)
            .takeIf { it }
            ?: throw CustomException(ErrorCode.FORBIDDEN)
        
        post.categoryId = request.categoryId
        post.title = request.title
        post.content = request.content
        post.thumbnailUrl = request.thumbnailUrl
        
        return PostResponse.from(post)
    }
    
    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        val post = postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }
        
        (post.userId == userId)
            .takeIf { it }
            ?: throw CustomException(ErrorCode.FORBIDDEN)
        
        postRepository.delete(post)
    }
}
