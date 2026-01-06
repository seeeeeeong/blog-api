package com.blog.api.core.domain

import com.blog.api.core.integration.redis.RedisBaseService
import com.blog.api.storage.db.core.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
class PostViewService(
    private val postRepository: PostRepository,
    private val redisBaseService: RedisBaseService
) {

    companion object {
        private const val VIEW_KEY_PREFIX = "post:view:"
        private const val VIEW_EXPIRATION_HOURS = 1L
        private const val VIEW_COUNT_VALUE = "1"
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun incrementViewCountOnce(postId: Long, clientIp: String) {
        val viewKey = "$VIEW_KEY_PREFIX$postId:$clientIp"

        if (redisBaseService.get(viewKey) == null) {
            postRepository.incrementViewCount(postId)
            redisBaseService.set(viewKey, VIEW_COUNT_VALUE, VIEW_EXPIRATION_HOURS, TimeUnit.HOURS)
        }
    }
}
