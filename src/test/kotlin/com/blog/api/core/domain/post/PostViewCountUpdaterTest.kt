package com.blog.api.core.domain.post

import com.blog.api.storage.post.PostRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class PostViewCountUpdaterTest {

    @Test
    fun `이벤트 수신 시 조회수를 증가시킨다`() {
        val postRepository = mock(PostRepository::class.java)
        val updater = PostViewCountUpdater(postRepository)

        updater.handle(PostViewedEvent(postId = 1L))

        verify(postRepository).incrementViewCount(1L)
    }
}
