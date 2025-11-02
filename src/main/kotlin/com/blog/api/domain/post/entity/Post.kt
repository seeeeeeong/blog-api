package com.blog.api.domain.post.entity

import com.blog.api.global.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
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
    var viewCount: Int = 0

) : BaseTimeEntity() {

    fun increaseViewCount() {
        this.viewCount++
    }
}
