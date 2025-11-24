package com.blog.api.global.security

import com.blog.api.global.config.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())

    companion object {
        private const val GITHUB_USERNAME_CLAIM = "githubUsername"
        private const val GITHUB_AVATAR_URL_CLAIM = "githubAvatarUrl"
    }

    fun generateAccessToken(userId: Long): String {
        val now = Date()
        val expiration = Date(now.time + jwtProperties.accessExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    fun generateGitHubAccessToken(
        githubId: Long,
        githubUsername: String,
        githubAvatarUrl: String?
    ): String {
        val now = Date()
        val expiration = Date(now.time + jwtProperties.accessExpiration)

        return Jwts.builder()
            .subject(githubId.toString())
            .claim(GITHUB_USERNAME_CLAIM, githubUsername)
            .claim(GITHUB_AVATAR_URL_CLAIM, githubAvatarUrl)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    fun generateRefreshToken(userId: Long): String {
        val now = Date()
        val expiration = Date(now.time + jwtProperties.refreshExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    fun getUserIdFromToken(token: String): Long {
        return getClaims(token).subject.toLong()
    }

    fun getGitHubIdFromToken(token: String): String {
        return getClaims(token).subject
    }

    fun getGitHubUsernameFromToken(token: String): String {
        return getClaims(token).get(GITHUB_USERNAME_CLAIM, String::class.java)
    }

    fun getGitHubAvatarUrlFromToken(token: String): String? {
        return getClaims(token).get(GITHUB_AVATAR_URL_CLAIM, String::class.java)
    }

    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}