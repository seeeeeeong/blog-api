package com.blog.api.core.domain

import com.blog.api.core.support.converter.PostMarkdownConverter
import com.blog.api.storage.CommentRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentHtmlBackfillService(
    private val commentRepository: CommentRepository,
    private val postMarkdownConverter: PostMarkdownConverter,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun backfillContentHtml() {
        val comments = commentRepository.findAllWithNullHtml()
        if (comments.isEmpty()) return

        comments.forEach { comment ->
            comment.contentHtml = postMarkdownConverter.convertToHtml(comment.content)
        }
        logger.info("comment-backfill: {} comments backfilled", comments.size)
    }
}
