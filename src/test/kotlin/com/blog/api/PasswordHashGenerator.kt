package com.blog.api

import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class PasswordHashGenerator {

    @Test
    fun generateAdminPasswordHash() {
        val encoder = BCryptPasswordEncoder()
        val password = "1q2w3e4r"
        val encoded = encoder.encode(password)

        println("=".repeat(80))
        println("Password: $password")
        println("BCrypt Hash:")
        println(encoded)
        println("=".repeat(80))
    }
}
