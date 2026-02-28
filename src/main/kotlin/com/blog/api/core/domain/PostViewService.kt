package com.blog.api.core.domain

import com.blog.api.core.support.connector.RedisOps
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Service
class PostViewService(
    private val redisOps: RedisOps,
    @param:Value("\${view-count.ttl-seconds:3600}") private val ttlSeconds: Long,
    @param:Value("\${view-count.local-cache.max-entries:10000}") private val localCacheMaxEntries: Long,
    circuitBreakerRegistry: CircuitBreakerRegistry,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("view-count-redis")

    private val localCache: Cache<String, Long> = Caffeine.newBuilder()
        .maximumSize(localCacheMaxEntries)
        .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
        .build()

    companion object {
        private val SHA256 = ThreadLocal.withInitial { MessageDigest.getInstance("SHA-256") }
    }

    fun shouldIncrement(postId: Long, clientIp: String): Boolean {
        val key = buildKey(postId, clientIp)
        return try {
            CircuitBreaker.decorateSupplier(circuitBreaker) { shouldIncrementViaRedis(key) }.get()
        } catch (e: Exception) {
            logger.warn("view-count: Redis unavailable ({}), falling back to local cache", e.message)
            shouldIncrementViaLocalCache(key)
        }
    }

    private fun shouldIncrementViaRedis(key: String): Boolean {
        val result = redisOps.setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS)
        if (result.isError) throw RedisOperationException("setIfAbsent failed: key=$key")
        return result.isSet
    }

    private fun shouldIncrementViaLocalCache(key: String): Boolean {
        if (localCache.getIfPresent(key) != null) return false
        localCache.put(key, System.currentTimeMillis())
        return true
    }

    private fun buildKey(postId: Long, clientIp: String): String {
        val hash = SHA256.get()
            .digest(clientIp.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
        return "post:view:$postId:$hash"
    }

    private class RedisOperationException(message: String) : RuntimeException(message)
}
