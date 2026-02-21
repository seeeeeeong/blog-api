package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.LoginRateLimitProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Service
class LoginRateLimitService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val properties: LoginRateLimitProperties,
) {
    companion object {
        private const val UNKNOWN_IP = "unknown"
        private const val IP_KEY_PREFIX = "login:fail:ip:"
        private const val EMAIL_KEY_PREFIX = "login:fail:email:"
        private const val PAIR_KEY_PREFIX = "login:fail:pair:"
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    fun checkOrThrow(clientIp: String, email: String) {
        val keySet = buildKeySet(clientIp, email)
        val counters = readCounters(keySet)
        val isBlocked = counters.ip >= properties.maxFailuresPerIp ||
            counters.email >= properties.maxFailuresPerEmail ||
            counters.pair >= properties.maxFailuresPerIpEmail

        if (isBlocked) {
            throw CoreException(ErrorType.LOGIN_RATE_LIMITED)
        }
    }

    fun recordFailure(clientIp: String, email: String) {
        val keySet = buildKeySet(clientIp, email)
        withFailClosed("recordFailure") {
            incrementWithTtl(keySet.ip)
            incrementWithTtl(keySet.email)
            incrementWithTtl(keySet.pair)
        }
    }

    fun clearFailures(clientIp: String, email: String) {
        val keySet = buildKeySet(clientIp, email)
        withFailClosed("clearFailures") {
            redisTemplate.delete(listOf(keySet.ip, keySet.email, keySet.pair))
        }
    }

    private fun readCounters(keySet: KeySet): Counters {
        return withFailClosed("readCounters") {
            Counters(
                ip = readCounter(keySet.ip),
                email = readCounter(keySet.email),
                pair = readCounter(keySet.pair),
            )
        }
    }

    private fun incrementWithTtl(key: String) {
        val current = redisTemplate.opsForValue().increment(key)
        if (current == null) {
            throw IllegalStateException("Redis increment returned null")
        }
        if (current == 1L) {
            redisTemplate.expire(key, properties.windowSeconds, TimeUnit.SECONDS)
        }
    }

    private fun readCounter(key: String): Long {
        val value = redisTemplate.opsForValue().get(key) ?: return 0L
        return value.toLongOrNull() ?: 0L
    }

    private fun buildKeySet(clientIp: String, email: String): KeySet {
        val normalizedIp = normalizeIp(clientIp)
        val normalizedEmail = normalizeEmail(email)
        val ipHash = hash(normalizedIp)
        val emailHash = hash(normalizedEmail)
        return KeySet(
            ip = "$IP_KEY_PREFIX$ipHash",
            email = "$EMAIL_KEY_PREFIX$emailHash",
            pair = "$PAIR_KEY_PREFIX$ipHash:$emailHash",
        )
    }

    private fun normalizeIp(clientIp: String): String {
        val trimmed = clientIp.trim()
        if (trimmed.isNotBlank()) {
            return trimmed
        }
        return UNKNOWN_IP
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun hash(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    private inline fun <T> withFailClosed(action: String, block: () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            logger.error("login-rate-limit.$action failed", e)
            throw CoreException(ErrorType.LOGIN_RATE_LIMITED)
        }
    }

    private data class KeySet(
        val ip: String,
        val email: String,
        val pair: String,
    )

    private data class Counters(
        val ip: Long,
        val email: Long,
        val pair: Long,
    )
}
