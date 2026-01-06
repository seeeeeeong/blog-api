package com.blog.api.core.integration.embedding

import com.blog.api.core.api.config.properties.EmbeddingProperties
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EmbeddingService(
    private val embeddingProvider: EmbeddingProvider,
    private val embeddingProperties: EmbeddingProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Create embedding vector from text
     * - Preprocesses text (truncation, markdown removal)
     * - Generates embedding using OpenAI API
     * @param text Raw text to embed
     * @return FloatArray embedding vector
     * @throws CoreException if embedding generation fails
     */
    fun createEmbedding(text: String): FloatArray {
        return try {
            val processedText = preprocess(text)
            embeddingProvider.generateEmbedding(processedText)
        } catch (e: Exception) {
            logger.error("Failed to create embedding", e)
            when (e) {
                is CoreException -> throw e
                else -> throw CoreException(ErrorType.EMBEDDING_GENERATION_FAILED)
            }
        }
    }

    private fun preprocess(text: String): String {
        val truncatedText = truncateIfNeeded(text)
        val cleanText = removeMarkdownSyntax(truncatedText)

        return if (cleanText.isBlank()) {
            logger.warn("Clean text is blank after markdown removal. Using original text.")
            truncatedText
        } else {
            cleanText
        }
    }

    private fun truncateIfNeeded(text: String): String {
        val maxLength = embeddingProperties.maxContentLength
        return if (text.length > maxLength) {
            logger.warn("Content exceeds max length. Truncating from ${text.length} to $maxLength chars")
            text.substring(0, maxLength)
        } else {
            text
        }
    }

    private fun removeMarkdownSyntax(markdown: String): String {
        return markdown
            .replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("`[^`]+`"), "")
            .replace(Regex("#{1,6}\\s"), "")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("\\*(.+?)\\*"), "$1")
            .replace(Regex("\\[(.+?)\\]\\(.+?\\)"), "$1")
            .replace(Regex("!\\[.*?\\]\\(.+?\\)"), "")
            .replace(Regex("^>\\s", RegexOption.MULTILINE), "")
            .replace(Regex("^[-*+]\\s", RegexOption.MULTILINE), "")
            .replace(Regex("\\n+"), " ")
            .trim()
    }
}
