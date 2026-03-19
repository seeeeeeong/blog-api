package com.blog.api.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PostTopicHintExtractorTest {

    @Test
    fun `extracts strong technical topic hints from title and content`() {
        val title = "Kafka Consumer로 최종 발급 확정과 멱등성 보장하기"
        val content = """
            Redis Lua Script와 Outbox Pattern으로 쿠폰 발급 이벤트를 저장한 뒤,
            Kafka Consumer에서 최종 발급을 확정하고 멱등성을 보장했다.
            Terraform으로 운영 인프라도 함께 정리했다.
        """.trimIndent()

        val hints = PostTopicHintExtractor.extract(title, content)

        assertThat(hints).contains("Kafka Consumer", "Idempotency", "Redis Lua Script", "Outbox Pattern", "Terraform")
    }

    @Test
    fun `strips markdown and code blocks before extracting hints`() {
        val title = "Redis 장애를 사용자 장애로 만들지 않는 법"
        val content = """
            ## 장애 대응
            ```kotlin
            val redis = "ignore me"
            ```
            [Circuit Breaker](https://example.com)와 fallback 전략으로 Redis 장애를 격리했다.
        """.trimIndent()

        val hints = PostTopicHintExtractor.extract(title, content)

        assertThat(hints).contains("Redis", "Failure Handling", "Circuit Breaker")
    }
}
