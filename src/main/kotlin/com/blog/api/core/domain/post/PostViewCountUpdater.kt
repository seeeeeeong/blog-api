package com.blog.api.core.domain.post

import com.blog.api.storage.post.PostRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PostViewCountUpdater(
    private val postRepository: PostRepository,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onPostViewed(event: PostViewedEvent) {
        postRepository.incrementViewCount(event.postId)
    }
}
