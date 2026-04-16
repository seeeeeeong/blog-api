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
    fun `findRootCommentsByPostId returns only active root comments`() {
        val active = commentRepository.save(
            CommentEntity(
                postId = 1L,
                nickname = "user1",
                password = "pw",
                parentId = null,
                content = "active root",
            ),
        )
        val deleted = commentRepository.save(
            CommentEntity(
                postId = 1L,
                nickname = "user2",
                password = "pw",
                parentId = null,
                content = "deleted root",
            ),
        )
        deleted.delete()
        entityManager.flush()
        entityManager.clear()

        val result = commentRepository.findRootCommentsByPostId(1L)

        assertEquals(1, result.size)
        assertEquals(active.id, result[0].id)
    }

    @Test
    fun `findReplyCommentsByPostId returns only active reply comments`() {
        val root = commentRepository.save(
            CommentEntity(
                postId = 1L,
                nickname = "user1",
                password = "pw",
                parentId = null,
                content = "root",
            ),
        )
        val activeReply = commentRepository.save(
            CommentEntity(
                postId = 1L,
                nickname = "user2",
                password = "pw",
                parentId = root.id,
                content = "active reply",
            ),
        )
        val deletedReply = commentRepository.save(
            CommentEntity(
                postId = 1L,
                nickname = "user3",
                password = "pw",
                parentId = root.id,
                content = "deleted reply",
            ),
        )
        deletedReply.delete()
        entityManager.flush()
        entityManager.clear()

        val result = commentRepository.findReplyCommentsByPostId(1L)

        assertEquals(1, result.size)
        assertEquals(activeReply.id, result[0].id)
    }

    @Test
    fun `findRootCommentsByPostId returns empty list for post with no comments`() {
        val result = commentRepository.findRootCommentsByPostId(999L)
        assertTrue(result.isEmpty())
    }
}
