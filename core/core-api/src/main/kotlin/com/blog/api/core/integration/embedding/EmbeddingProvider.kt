package com.blog.api.core.integration.embedding

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.stereotype.Component

@Component
class EmbeddingProvider(
    private val embeddingModel: OpenAiEmbeddingModel
) {

    /**
     * Generate embedding vector from text using OpenAI API
     * @param text Preprocessed text
     * @return FloatArray embedding vector
     * @throws CoreException if embedding generation fails
     */
    fun generateEmbedding(text: String): FloatArray {
        val response = embeddingModel.embedForResponse(listOf(text))

        val embedding = response.results.firstOrNull()?.output
            ?: throw CoreException(ErrorType.EMBEDDING_GENERATION_FAILED)

        return embedding.map { value -> value.toFloat() }.toFloatArray()
    }
}
