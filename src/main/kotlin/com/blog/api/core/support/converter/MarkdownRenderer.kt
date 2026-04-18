package com.blog.api.core.support.converter

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils

@Component
class MarkdownRenderer(
    private val postMarkdownConverter: PostMarkdownConverter,
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun renderOrEscape(
        content: String,
        contextId: Long? = null,
    ): String =
        try {
            postMarkdownConverter.convertToHtml(content)
        } catch (e: Exception) {
            log.warn(e) { "Markdown conversion failed: contextId=$contextId" }
            HtmlUtils.htmlEscape(content)
        }
}
