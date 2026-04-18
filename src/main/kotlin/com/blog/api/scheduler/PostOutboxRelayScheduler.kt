package com.blog.api.scheduler

import com.blog.api.core.domain.postoutbox.PostOutboxRelayService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PostOutboxRelayScheduler(
    private val postOutboxRelayService: PostOutboxRelayService,
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Scheduled(fixedDelayString = "\${blog-ai.relay.interval-ms:5000}")
    fun relay() {
        try {
            val ids = postOutboxRelayService.fetchPendingIds()
            if (ids.isEmpty()) return
            ids.forEach(postOutboxRelayService::relayOne)
        } catch (e: Exception) {
            log.error(e) { "Outbox relay sweep failed" }
        }
    }
}
