package com.blog.api.storage.category

import com.blog.api.storage.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
class CategoryEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    name: String,
    slug: String,
) : BaseTimeEntity() {

    @Column(nullable = false, unique = true, length = 50)
    var name: String = name
        protected set

    @Column(nullable = false, unique = true, length = 50)
    var slug: String = slug
        protected set
}
