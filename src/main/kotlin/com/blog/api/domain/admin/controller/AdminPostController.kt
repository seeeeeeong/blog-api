package com.blog.api.domain.admin.controller

import com.blog.api.domain.post.dto.PostListResponse
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.global.response.ApiResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/posts")
class AdminPostController(
    private val postRepository: PostRepository
) {

    @GetMapping
    fun getAllPosts(
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): ApiResponse<PostListResponse> {
        val postPage = postRepository.findAll(pageable)
        return ApiResponse.success(PostListResponse.from(postPage))
    }

    @DeleteMapping("/{postId}")
    fun deletePost(@PathVariable postId: Long): ApiResponse<Unit> {
        postRepository.deleteById(postId)
        return ApiResponse.success()
    }
}