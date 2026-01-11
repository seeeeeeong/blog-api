package com.blog.api.storage

import com.blog.api.storage.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "comments",
    indexes = [
        Index(name = "idx_comment_post_deleted_created", columnList = "post_id,is_deleted,created_at")
    ]
)
class CommentEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "github_id", nullable = false, length = 50)
    val oauthId: String,

    @Column(name = "github_username", nullable = false, length = 100)
    val oauthUsername: String,

    @Column(name = "github_avatar_url", length = 500)
    val oauthAvatarUrl: String? = null,

    @Column(name = "parent_id")
    val parentId: Long? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "is_deleted", nullable = false)
    private var isDeleted: Boolean = false

) : BaseTimeEntity() {

    fun updateContent(newContent: String) {
        this.content = newContent
    }

    fun delete() {
        this.isDeleted = true
    }

    fun isActive(): Boolean = !isDeleted
}
