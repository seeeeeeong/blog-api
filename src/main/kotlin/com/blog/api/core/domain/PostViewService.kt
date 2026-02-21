package com.blog.api.core.domain

import com.blog.api.core.support.connector.RedisOps
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Service
class PostViewService(
    private val redisOps: RedisOps,
    @param:Value("\${view-count.ttl-seconds:3600}") private val ttlSeconds: Long,
    @param:Value("\${view-count.circuit-breaker.failure-threshold:3}") private val failureThreshold: Int,
    @param:Value("\${view-count.circuit-breaker.open-duration-ms:30000}") private val openDurationMs: Long,
    @param:Value("\${view-count.local-cache.max-entries:10000}") private val localCacheMaxEntries: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val consecutiveFailures = AtomicInteger(0)
    private val circuitOpenUntil = AtomicLong(0)
    private val localCache = ConcurrentHashMap<String, Long>()
    private val localCacheTouchCount = AtomicLong(0)

    companion object {
        private const val LOCAL_CACHE_CLEANUP_INTERVAL = 256L
    }

    fun shouldIncrement(postId: Long, clientIp: String): Boolean {
        val key = buildKey(postId, clientIp)

        if (isCircuitOpen()) {
            return shouldIncrementFromLocalCache(key)
        }

        val redisResult = redisOps.setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS)
        if (redisResult.isError) {
            return handleRedisFailure(key)
        }

        consecutiveFailures.set(0)
        return redisResult.isSet
    }

    private fun isCircuitOpen() = System.currentTimeMillis() < circuitOpenUntil.get()

    private fun handleRedisFailure(key: String): Boolean {
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            openCircuit()
        }
        return shouldIncrementFromLocalCache(key)
    }

    private fun openCircuit() {
        circuitOpenUntil.set(System.currentTimeMillis() + openDurationMs)
        consecutiveFailures.set(0)
        logger.warn("view-count.circuit: open")
    }

    private fun shouldIncrementFromLocalCache(key: String): Boolean {
        val now = System.currentTimeMillis()
        val expiresAt = now + TimeUnit.SECONDS.toMillis(ttlSeconds)

        while (true) {
            val currentExpiresAt = localCache[key]
            if (currentExpiresAt == null) {
                if (localCache.putIfAbsent(key, expiresAt) == null) {
                    cleanupLocalCacheIfNeeded(now)
                    return true
                }
                continue
            }

            val isActive = currentExpiresAt > now
            if (isActive) {
                return false
            }

            if (localCache.replace(key, currentExpiresAt, expiresAt)) {
                cleanupLocalCacheIfNeeded(now)
                return true
            }
        }
    }

    private fun cleanupLocalCacheIfNeeded(now: Long) {
        val periodicCleanupDue = localCacheTouchCount.incrementAndGet() % LOCAL_CACHE_CLEANUP_INTERVAL == 0L
        val overMaxEntries = localCache.size > localCacheMaxEntries
        val shouldCleanup = periodicCleanupDue || overMaxEntries
        if (shouldCleanup) {
            localCache.entries.removeIf { (_, expiresAt) -> expiresAt <= now }
            if (localCache.size > localCacheMaxEntries) {
                val keyIterator = localCache.keys.iterator()
                while (localCache.size > localCacheMaxEntries && keyIterator.hasNext()) {
                    localCache.remove(keyIterator.next())
                }
            }
        }
    }

    private fun buildKey(postId: Long, clientIp: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(clientIp.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
        return "post:view:$postId:$hash"
    }
}
