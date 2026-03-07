package com.blog.api.core.api.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import kotlin.random.Random

@Configuration
@EnableCaching
class CacheConfig : CachingConfigurer {

    @Bean
    override fun cacheManager(): CacheManager {
        val jitterSeconds = Random.nextLong(0, 3600)
        val caffeine = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24).plusSeconds(jitterSeconds))
            .maximumSize(500)
        return CaffeineCacheManager("post-html").apply {
            setCaffeine(caffeine)
        }
    }
}
