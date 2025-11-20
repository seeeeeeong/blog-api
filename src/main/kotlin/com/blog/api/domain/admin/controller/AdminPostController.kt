package com.blog.api.domain.admin.controller

import com.blog.api.domain.post.dto.PostListResponse
import com.blog.api.domain.post.dto.PostResponse
import com.blog.api.domain.post.repository.PostRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/posts")
class AdminPostController(
    private val postRepository: PostRepository
) {

    @GetMapping
    fun getAllPosts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PostListResponse> {
        val pageable = PageRequest.of(page, size)
        val postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable)

        return ResponseEntity.ok(
            PostListResponse(
                posts = postPage.content.map { PostResponse.Companion.from(it) },
                totalPages = postPage.totalPages,
                totalElements = postPage.totalElements,
                currentPage = postPage.number,
                pageSize = postPage.size
            )
        )
    }

    @DeleteMapping("/{postId}")
    fun deletePost(@PathVariable postId: Long): ResponseEntity<Void> {
        postRepository.deleteById(postId)
        return ResponseEntity.noContent().build()
    }
}