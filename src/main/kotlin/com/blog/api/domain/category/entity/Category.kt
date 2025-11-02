package com.blog.api.domain.category.entity

import com.blog.api.global.entity.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(name = "categories")
class Category(
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, unique = true, length = 50)
    var name: String,
    
    @Column(nullable = false, unique = true, length = 50)
    var slug: String  // URL용: spring-boot, kotlin, etc.
    
) : BaseTimeEntity()
