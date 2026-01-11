package com.blog.api.storage

import com.blog.api.core.enum.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    password: String,
    nickname: String,
    profileImageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: UserRole = UserRole.USER
) : BaseTimeEntity() {

    @Column(nullable = false)
    var password: String = password
        protected set

    @Column(nullable = false, unique = true, length = 50)
    var nickname: String = nickname
        protected set

    @Column(length = 500)
    var profileImageUrl: String? = profileImageUrl
        protected set

}