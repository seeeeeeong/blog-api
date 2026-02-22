package com.blog.api.core.api.config

import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.cache.interceptor.SimpleCacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
) : org.springframework.cache.annotation.CachingConfigurer {

    @Bean
    fun cacheManager(): RedisCacheManager {
        val htmlCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )

        return RedisCacheManager.builder(redisConnectionFactory)
            .withCacheConfiguration("post-html", htmlCacheConfig)
            .build()
    }

    override fun errorHandler(): CacheErrorHandler {
        return object : SimpleCacheErrorHandler() {
            private val log = LoggerFactory.getLogger(CacheConfig::class.java)

            override fun handleCacheGetError(e: RuntimeException, cache: Cache, key: Any) {
                log.warn("[Cache] GET 실패 - cache={}, key={}: {}", cache.name, key, e.message)
            }

            override fun handleCachePutError(e: RuntimeException, cache: Cache, key: Any, value: Any?) {
                log.warn("[Cache] PUT 실패 - cache={}, key={}: {}", cache.name, key, e.message)
            }

            override fun handleCacheEvictError(e: RuntimeException, cache: Cache, key: Any) {
                log.warn("[Cache] EVICT 실패 - cache={}, key={}: {}", cache.name, key, e.message)
            }
        }
    }
}
