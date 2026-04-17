package com.blog.api.storage.comment

import com.blog.api.storage.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(
    name = "comments",
    indexes = [
        Index(name = "idx_comment_post_deleted_created", columnList = "post_id,is_deleted,created_at"),
    ],
)
@SQLDelete(sql = "UPDATE comments SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
class CommentEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(nullable = false, length = 50)
    val nickname: String,

    content: String,
    contentHtml: String? = null,

    @Column(name = "is_deleted", nullable = false)
    private var deleted: Boolean = false,

) : BaseTimeEntity() {

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String = content
        protected set

    @Column(name = "content_html", columnDefinition = "TEXT")
    var contentHtml: String? = contentHtml
        protected set

    fun delete() {
        this.deleted = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as CommentEntity
        return id != null && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
