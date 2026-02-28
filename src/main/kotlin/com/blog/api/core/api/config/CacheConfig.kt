package com.blog.api.core.api.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
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
) : CachingConfigurer {

    @Bean
    override fun cacheManager(): CacheManager {
        val htmlCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )

        return RedisCacheManager.builder(redisConnectionFactory)
            .withCacheConfiguration("post-html", htmlCacheConfig)
            .build()
    }
}
