package com.blog.api.domain.post.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "post_tags",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["post_id", "tag_id"])
    ]
)
class PostTag(
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "post_id", nullable = false)
    val postId: Long,
    
    @Column(name = "tag_id", nullable = false)
    val tagId: Long
)
