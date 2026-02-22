package com.blog.api.core.domain

import com.blog.api.core.support.converter.PostMarkdownConverter
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class PostHtmlCacheService(
    private val postMarkdownConverter: PostMarkdownConverter,
) {

    @Cacheable(cacheNames = ["post-html"], key = "#postId")
    fun getHtml(postId: Long, markdown: String): String {
        return postMarkdownConverter.convertToHtml(markdown)
    }

    @CacheEvict(cacheNames = ["post-html"], key = "#postId")
    fun evict(postId: Long) {
    }
}
