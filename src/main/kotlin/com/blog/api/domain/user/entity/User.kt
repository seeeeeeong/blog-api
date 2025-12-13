package com.blog.api.domain.user.entity

import com.blog.api.common.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 100)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false, unique = true, length = 50)
    var nickname: String,

    @Column(length = 500)
    var profileImageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER

) : BaseTimeEntity()
