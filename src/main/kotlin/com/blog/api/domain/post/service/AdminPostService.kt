package com.blog.api.domain.post.service

import com.blog.api.domain.post.dto.PostListResponse
import com.blog.api.domain.post.repository.PostRepository
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import com.blog.api.global.util.MarkdownUtil
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPostService(
    private val postRepository: PostRepository,
    private val markdownUtil: MarkdownUtil
) {

    fun getAllPosts(pageable: Pageable): PostListResponse =
        PostListResponse.from(postRepository.findAll(pageable), markdownUtil::convertToHtml)

    @Transactional
    fun deletePost(postId: Long) {
        val post = postRepository.findById(postId)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }

        postRepository.delete(post)
    }
}
