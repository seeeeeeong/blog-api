package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.LoginRateLimitProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import java.security.MessageDigest

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

        private val SHA256 = ThreadLocal.withInitial { MessageDigest.getInstance("SHA-256") }

        private val RECORD_FAILURE_SCRIPT = DefaultRedisScript<Long>().apply {
            setScriptText(
                """
                for i = 1, #KEYS do
                    local current = redis.call('INCR', KEYS[i])
                    if current == 1 then
                        redis.call('EXPIRE', KEYS[i], ARGV[1])
                    end
                end
                return 1
                """.trimIndent()
            )
            setResultType(Long::class.java)
        }
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
        try {
            redisTemplate.execute(
                RECORD_FAILURE_SCRIPT,
                listOf(keySet.ip, keySet.email, keySet.pair),
                properties.windowSeconds.toString(),
            )
        } catch (e: Exception) {
            // fail-open: Redis 장애 시 기록 실패를 허용. checkOrThrow의 fail-closed가 보안 경계를 지킴.
            logger.error("login-rate-limit.recordFailure failed: {}", e.message)
        }
    }

    fun clearFailures(clientIp: String, email: String) {
        val keySet = buildKeySet(clientIp, email)
        try {
            // IP 카운터는 초기화하지 않음: 다른 계정의 실패 이력까지 함께 리셋되는 것을 방지
            redisTemplate.delete(listOf(keySet.email, keySet.pair))
        } catch (e: Exception) {
            logger.warn("login-rate-limit.clearFailures failed: {}", e.message)
        }
    }

    private fun readCounters(keySet: KeySet): Counters {
        return withFailClosed("readCounters") {
            val values = redisTemplate.opsForValue().multiGet(listOf(keySet.ip, keySet.email, keySet.pair))
            Counters(
                ip = values?.get(0)?.toLongOrNull() ?: 0L,
                email = values?.get(1)?.toLongOrNull() ?: 0L,
                pair = values?.get(2)?.toLongOrNull() ?: 0L,
            )
        }
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
        return SHA256.get()
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
