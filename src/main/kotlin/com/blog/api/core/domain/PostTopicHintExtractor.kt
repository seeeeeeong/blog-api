package com.blog.api.core.domain

object PostTopicHintExtractor {
    private val aliasToTopic = linkedMapOf(
        "materialized view" to "Materialized View",
        "mview" to "Materialized View",
        "redis lua script" to "Redis Lua Script",
        "redis" to "Redis",
        "kafka consumer" to "Kafka Consumer",
        "idempotency" to "Idempotency",
        "멱등성" to "Idempotency",
        "terraform" to "Terraform",
        "outbox pattern" to "Outbox Pattern",
        "kafka" to "Kafka",
        "outbox" to "Outbox Pattern",
        "postgresql" to "PostgreSQL",
        "postgres" to "PostgreSQL",
        "oauth" to "OAuth",
        "jwt" to "JWT",
        "cloudfront" to "CloudFront",
        "s3" to "S3",
        "docker" to "Docker",
        "ec2" to "EC2",
        "rds" to "RDS",
        "prometheus" to "Prometheus",
        "grafana" to "Grafana",
        "circuit breaker" to "Circuit Breaker",
        "쿠폰" to "Coupon System",
        "coupon" to "Coupon System",
        "장애" to "Failure Handling",
        "fallback" to "Failure Handling",
        "성능" to "Performance",
        "performance" to "Performance",
        "t4g.micro" to "t4g.micro",
        "t4g" to "t4g.micro",
    )

    private val stopWords = setOf(
        "this", "that", "with", "from", "into", "through", "about", "after", "before", "when",
        "where", "using", "used", "have", "will", "your", "their", "there", "system", "service",
        "story", "guide", "reason", "design", "issue", "problem", "solution", "운영기", "이유",
        "정리", "구현", "문제", "해결", "개발", "적용", "구조", "방법", "그리고", "통해",
    )

    fun extract(title: String, content: String, limit: Int = 6): List<String> {
        val normalizedTitle = normalize(title)
        val normalizedContent = normalize(content)
        val combined = "$normalizedTitle $normalizedContent"
        val hints = linkedSetOf<String>()

        aliasToTopic.forEach { (alias, topic) ->
            if (alias in combined && hints.none { topic in it || it in topic }) {
                hints += topic
            }
        }

        val titleTokens = scoreTokens(normalizedTitle, weight = 3)
        val bodyTokens = scoreTokens(normalizedContent, weight = 1)
        val mergedTokens = linkedMapOf<String, Int>()

        titleTokens.forEach { (token, score) ->
            mergedTokens[token] = (mergedTokens[token] ?: 0) + score
        }
        bodyTokens.forEach { (token, score) ->
            mergedTokens[token] = (mergedTokens[token] ?: 0) + score
        }

        mergedTokens.entries
            .sortedByDescending { it.value }
            .map { tokenToTopicLabel(it.key) }
            .filter { it.isNotBlank() }
            .forEach { topic ->
                if (hints.none { topic in it || it in topic }) {
                    hints += topic
                }
            }

        return hints.take(limit).toList()
    }

    private fun scoreTokens(text: String, weight: Int): Map<String, Int> {
        return text.split(Regex("[^\\p{L}\\p{N}.]+"))
            .asSequence()
            .map { it.trim('.') }
            .filter { it.length >= 3 }
            .filterNot { it in stopWords }
            .groupingBy { it }
            .eachCount()
            .mapValues { (_, count) -> count * weight }
    }

    private fun tokenToTopicLabel(token: String): String {
        return when (token) {
            "redis" -> "Redis"
            "kafka" -> "Kafka"
            "terraform" -> "Terraform"
            "postgres", "postgresql" -> "PostgreSQL"
            "oauth" -> "OAuth"
            "jwt" -> "JWT"
            "docker" -> "Docker"
            "cloudfront" -> "CloudFront"
            "prometheus" -> "Prometheus"
            "grafana" -> "Grafana"
            "outbox" -> "Outbox Pattern"
            "coupon", "쿠폰" -> "Coupon System"
            "idempotency", "멱등성" -> "Idempotency"
            "performance", "성능" -> "Performance"
            "장애", "fallback" -> "Failure Handling"
            else -> token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace(Regex("`[^`]*`"), " ")
            .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), " ")
            .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
            .replace(Regex("https?://\\S+"), " ")
            .replace(Regex("[*_>#]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
