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

    @Column(name = "github_id", nullable = false, length = 50)
    val githubId: String,

    @Column(name = "github_username", nullable = false, length = 100)
    val githubUsername: String,

    @Column(name = "github_avatar_url", length = 500)
    val githubAvatarUrl: String? = null,

    @Column(name = "parent_id")
    val parentId: Long? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String

) : BaseTimeEntity() {
    fun isReply(): Boolean = parentId != null

    fun isAuthor(githubId: String): Boolean = this.githubId == githubId
}