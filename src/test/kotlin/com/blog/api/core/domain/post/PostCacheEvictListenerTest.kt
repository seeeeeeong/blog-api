package com.blog.api.core.domain.post

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

class PostCacheEvictListenerTest {
    @Test
    fun `이벤트 수신 시 post-html 캐시를 무효화한다`() {
        val cacheManager = mock(CacheManager::class.java)
        val htmlCache = mock(Cache::class.java)
        `when`(cacheManager.getCache("post-html")).thenReturn(htmlCache)
        val listener = PostCacheEvictListener(cacheManager)

        listener.onPostChanged(PostCacheEvictEvent(postId = 1L))

        verify(htmlCache).evict(1L)
    }
}
