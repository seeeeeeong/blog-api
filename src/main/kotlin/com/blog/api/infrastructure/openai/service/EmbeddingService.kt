package com.blog.api.infrastructure.openai.service

import com.blog.api.common.exception.CustomException
import com.blog.api.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.stereotype.Service


@Service
class EmbeddingService(
    private val embeddingModel: OpenAiEmbeddingModel
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_TOKENS = 8191
        private const val APPROX_CHARS_PER_TOKEN = 4
        private const val MAX_CONTENT_LENGTH = MAX_TOKENS * APPROX_CHARS_PER_TOKEN
    }


    fun createEmbedding(text: String): FloatArray {
        try {
            val processedText = if (text.length > MAX_CONTENT_LENGTH) {
                logger.warn("Content exceeds max length. Truncating from ${text.length} to $MAX_CONTENT_LENGTH chars")
                text.substring(0, MAX_CONTENT_LENGTH)
            } else {
                text
            }

            val cleanText = removeMarkdownSyntax(processedText)

            if (cleanText.isBlank()) {
                logger.warn("Clean text is blank after markdown removal. Using original text.")
                return createEmbeddingFromText(processedText)
            }

            return createEmbeddingFromText(cleanText)

        } catch (e: Exception) {
            logger.error("Failed to create embedding", e)
            when (e) {
                is CustomException -> throw e
                else -> throw CustomException(ErrorCode.EMBEDDING_GENERATION_FAILED)
            }
        }
    }

    private fun createEmbeddingFromText(text: String): FloatArray {
        val response = embeddingModel.embedForResponse(listOf(text))

        val embedding = response.results.firstOrNull()?.output
            ?: throw CustomException(ErrorCode.EMBEDDING_GENERATION_FAILED)

        return embedding.map { it.toFloat() }.toFloatArray()
    }


    private fun removeMarkdownSyntax(markdown: String): String {
        return markdown
            .replace(Regex("```[\\s\\S]*?```"), "") // 코드 블록 제거
            .replace(Regex("`[^`]+`"), "") // 인라인 코드 제거
            .replace(Regex("#{1,6}\\s"), "") // 헤더 제거
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1") // Bold 제거
            .replace(Regex("\\*(.+?)\\*"), "$1") // Italic 제거
            .replace(Regex("\\[(.+?)\\]\\(.+?\\)"), "$1") // 링크 제거
            .replace(Regex("!\\[.*?\\]\\(.+?\\)"), "") // 이미지 제거
            .replace(Regex("^>\\s", RegexOption.MULTILINE), "") // 인용 제거
            .replace(Regex("^[-*+]\\s", RegexOption.MULTILINE), "") // 리스트 제거
            .replace(Regex("\\n+"), " ") // 여러 줄바꿈을 공백으로
            .trim()
    }
}
