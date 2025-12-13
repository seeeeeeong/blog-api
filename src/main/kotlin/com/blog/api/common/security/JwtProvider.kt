package com.blog.api.common.security

import com.blog.api.common.exception.CustomException
import com.blog.api.common.exception.ErrorCode
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}")
    private val secret: String,
    @Value("\${jwt.access-expiration}")
    private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}")
    private val refreshExpiration: Long
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    companion object {
        private const val ROLE_CLAIM = "role"
        private const val GITHUB_USERNAME_CLAIM = "githubUsername"
        private const val GITHUB_AVATAR_URL_CLAIM = "githubAvatarUrl"
    }

    fun generateAccessToken(userId: Long, role: String): String {
        return generateToken(userId.toString(), accessExpiration) {
            it.claim(ROLE_CLAIM, role)
        }
    }

    fun generateRefreshToken(userId: Long, role: String): String {
        return generateToken(userId.toString(), refreshExpiration) {
            it.claim(ROLE_CLAIM, role)
        }
    }

    fun generateGitHubAccessToken(githubId: Long, githubUsername: String, githubAvatarUrl: String?): String {
        return generateToken(githubId.toString(), accessExpiration) {
            it.claim(GITHUB_USERNAME_CLAIM, githubUsername)
            it.claim(GITHUB_AVATAR_URL_CLAIM, githubAvatarUrl)
        }
    }

    private fun generateToken(subject: String, expirationMillis: Long, claims: (io.jsonwebtoken.JwtBuilder) -> Unit): String {
        val nowInstant = Instant.now()
        val expirationInstant = nowInstant.plusMillis(expirationMillis)

        val builder = Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(nowInstant))
            .expiration(Date.from(expirationInstant))
            .signWith(secretKey)

        claims(builder)
        return builder.compact()
    }


    fun parseClaims(token: String): Claims {
        return runCatching {
            getClaims(token)
        }.getOrElse {
            throw CustomException(ErrorCode.INVALID_TOKEN)
        }
    }

    fun getUserIdFromToken(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    fun getRoleFromToken(token: String): String {
        return parseClaims(token).get(ROLE_CLAIM, String::class.java)
            ?: throw CustomException(ErrorCode.INVALID_TOKEN)
    }

    fun getGitHubIdFromToken(token: String): String = parseClaims(token).subject

    fun getGitHubUsernameFromToken(token: String): String {
        return parseClaims(token).get(GITHUB_USERNAME_CLAIM, String::class.java)
            ?: throw CustomException(ErrorCode.INVALID_TOKEN)
    }

    fun getGitHubAvatarUrlFromToken(token: String): String? {
        return parseClaims(token).get(GITHUB_AVATAR_URL_CLAIM, String::class.java)
    }

    fun validateToken(token: String): Boolean {
        return runCatching {
            parseClaims(token)
        }.isSuccess
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}