package com.blog.api.domain.comment.entity

import com.blog.api.global.entity.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(name = "comments")
class Comment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "parent_id")
    val parentId: Long? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String

) : BaseTimeEntity() {
    fun isReply(): Boolean = parentId != null
}
