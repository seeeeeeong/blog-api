package com.blog.api.storage.post

import com.blog.api.core.enum.PostStatus
import com.blog.api.storage.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity
@Table(
    name = "posts",
    indexes = [
        Index(name = "idx_post_user_id", columnList = "user_id"),
        Index(name = "idx_post_category_id", columnList = "category_id"),
        Index(name = "idx_post_status", columnList = "status"),
        Index(name = "idx_post_created_at", columnList = "created_at"),
        Index(name = "idx_post_user_status", columnList = "user_id,status"),
        Index(name = "idx_post_category_status", columnList = "category_id,status"),
    ]
)
class PostEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val userId: Long,
    categoryId: Long,
    title: String,
    content: String,
    thumbnailUrl: String? = null,

    @Column(nullable = false)
    val viewCount: Int = 0,

    status: PostStatus = PostStatus.PUBLISHED,
) : BaseTimeEntity() {

    @Column(nullable = false)
    var categoryId: Long = categoryId
        protected set

    @Column(nullable = false, length = 200)
    var title: String = title
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String = content
        protected set

    @Column(length = 500)
    var thumbnailUrl: String? = thumbnailUrl
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PostStatus = status
        protected set

    fun updateContent(
        categoryId: Long,
        title: String,
        content: String,
        thumbnailUrl: String?,
        status: PostStatus
    ) {
        this.categoryId = categoryId
        this.title = title
        this.content = content
        this.thumbnailUrl = thumbnailUrl
        this.status = status
    }

    fun softDelete() {
        this.status = PostStatus.DELETED
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as PostEntity
        return id != null && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
