package com.blog.api.core.domain

import com.blog.api.storage.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.blog.api.core.support.connector.RedisOps

@Service
class PostViewBatchService(
    private val redisOps: RedisOps,
    private val postRepository: PostRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${view-count.batch-flush-ms:30000}")
    @Transactional
    fun flushPendingViewCounts() {
        val entries = redisOps.zrangeWithScores(PostViewService.BATCH_KEY, 0, -1)
        if (entries.isNullOrEmpty()) return

        var flushed = 0
        for (entry in entries) {
            val postId = entry.value?.toLongOrNull() ?: continue
            val delta = entry.score?.toLong()?.takeIf { it > 0 } ?: continue

            postRepository.incrementViewCountBy(postId, delta.toInt())
            redisOps.zincrby(PostViewService.BATCH_KEY, -delta.toDouble(), postId.toString())
            flushed++
        }

        if (flushed > 0) {
            logger.info("view-count: flushed {} posts to DB", flushed)
        }
    }
}
