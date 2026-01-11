package com.blog.api.core.support.connector

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.stereotype.Component

@Component
class EmbeddingClient(
    private val embeddingModel: OpenAiEmbeddingModel
) {

    fun generateEmbedding(text: String): FloatArray =
        embeddingModel.embedForResponse(listOf(text))
            .results
            .firstOrNull()
            ?.output
            ?: throw CoreException(ErrorType.EMBEDDING_GENERATION_FAILED)
}
