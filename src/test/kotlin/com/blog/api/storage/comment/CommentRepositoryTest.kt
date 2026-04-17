package com.blog.api.storage.comment

import com.blog.api.storage.config.TestJpaConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(TestJpaConfig::class)
class CommentRepositoryTest @Autowired constructor(
    private val commentRepository: CommentRepository,
    private val entityManager: TestEntityManager,
) {

    @Test
    fun `findByPostId returns only active comments`() {
        val active = commentRepository.save(
            CommentEntity(
                postId = 1L,
                nickname = "행��한 고양이",
                content = "active comment",
            ),
        )
        val deleted = commentRepository.save(
            CommentEntity(
                postId = 1L,
                nickname = "용감한 사자",
                content = "deleted comment",
            ),
        )
        deleted.delete()
        entityManager.flush()
        entityManager.clear()

        val result = commentRepository.findByPostId(1L)

        assertEquals(1, result.size)
        assertEquals(active.id, result[0].id)
    }

    @Test
    fun `findByPostId returns empty list for post with no comments`() {
        val result = commentRepository.findByPostId(999L)
        assertTrue(result.isEmpty())
    }
}
