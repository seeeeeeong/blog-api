package com.blog.api.domain.user.repository

import com.blog.api.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    
    fun findByEmail(email: String): User?
    
    fun existsByEmail(email: String): Boolean
    
    fun existsByNickname(nickname: String): Boolean
}
