package com.blog.api.storage.postoutbox

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PostOutboxRepository : JpaRepository<PostOutboxEntity, Long> {
    @Query(
        """
        SELECT o FROM PostOutboxEntity o
        WHERE o.processedAt IS NULL
          AND o.nextAttemptAt <= :now
        ORDER BY o.id ASC
        """,
    )
    fun findPending(
        @Param("now") now: LocalDateTime,
        pageable: Pageable,
    ): List<PostOutboxEntity>
}
