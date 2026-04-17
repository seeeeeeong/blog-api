package com.blog.api.core.api.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig : CachingConfigurer {

    companion object {
        const val POST_HTML = "post-html"
        const val CATEGORIES = "categories"
    }

    @Bean
    override fun cacheManager(): CacheManager {
        return CaffeineCacheManager().apply {
            registerCustomCache(
                POST_HTML,
                Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofHours(24))
                    .maximumSize(500)
                    .build(),
            )
            registerCustomCache(
                CATEGORIES,
                Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofMinutes(10))
                    .maximumSize(50)
                    .build(),
            )
        }
    }
}
