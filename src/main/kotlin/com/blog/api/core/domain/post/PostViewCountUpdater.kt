package com.blog.api.core.domain.post

import com.blog.api.storage.post.PostRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class PostViewCountUpdater(
    private val postRepository: PostRepository,
) {

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun increment(postId: Long) {
        postRepository.incrementViewCount(postId)
    }
}
