package com.blog.api.core.support.connector

import com.blog.api.core.support.properties.EmbeddingProperties
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EmbeddingFacade(
    private val embeddingClient: EmbeddingClient,
    private val embeddingProperties: EmbeddingProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun createEmbedding(text: String): FloatArray {
        return try {
            val maxLength = embeddingProperties.maxContentLength
            val truncatedText = if (text.length > maxLength) {
                logger.warn("Content exceeds max length. Truncating from ${text.length} to $maxLength chars")
                text.substring(0, maxLength)
            } else {
                text
            }

            val cleanText = truncatedText
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

            val processedText = if (cleanText.isBlank()) {
                logger.warn("Clean text is blank after markdown removal. Using original text.")
                truncatedText
            } else {
                cleanText
            }

            embeddingClient.generateEmbedding(processedText)
        } catch (e: Exception) {
            logger.error("Failed to create embedding", e)
            when (e) {
                is CoreException -> throw e
                else -> throw CoreException(ErrorType.EMBEDDING_GENERATION_FAILED)
            }
        }
    }
}
