package com.blog.api.domain.post.entity

import com.blog.api.common.entity.BaseTimeEntity
import com.blog.api.common.jpa.VectorType
import jakarta.persistence.*
import org.hibernate.annotations.Type

@Entity
@Table(
    name = "posts",
    indexes = [
        Index(name = "idx_post_user_id", columnList = "user_id"),
        Index(name = "idx_post_category_id", columnList = "category_id"),
        Index(name = "idx_post_status", columnList = "status"),
        Index(name = "idx_post_created_at", columnList = "created_at")
    ]
)
class Post(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "category_id", nullable = false)
    var categoryId: Long,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(length = 500)
    var thumbnailUrl: String? = null,

    @Column(nullable = false)
    var viewCount: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PostStatus = PostStatus.PUBLISHED,

    // 벡터 임베딩 (1536차원 - text-embedding-3-small)
    @Type(VectorType::class)
    @Column(name = "content_vector", columnDefinition = "vector(1536)")
    var contentVector: FloatArray? = null

) : BaseTimeEntity() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Post) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}

