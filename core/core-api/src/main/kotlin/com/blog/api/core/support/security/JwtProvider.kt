package com.blog.api.core.support.security

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-expiration}") private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    companion object {
        private const val ROLE_CLAIM = "role"
        private const val TOKEN_ID_CLAIM = "tokenId"
        private const val GITHUB_USERNAME_CLAIM = "githubUsername"
        private const val GITHUB_AVATAR_URL_CLAIM = "githubAvatarUrl"
    }

    fun generateAccessToken(userId: Long, role: String): String =
        generateToken(userId.toString(), accessExpiration) { it.claim(ROLE_CLAIM, role) }

    fun generateRefreshToken(userId: Long, tokenId: String, role: String): String =
        generateToken(userId.toString(), refreshExpiration) {
            it.claim(ROLE_CLAIM, role)
            it.claim(TOKEN_ID_CLAIM, tokenId)
        }

    fun generateGitHubAccessToken(githubId: Long, githubUsername: String, githubAvatarUrl: String?): String =
        generateToken(githubId.toString(), accessExpiration) {
            it.claim(GITHUB_USERNAME_CLAIM, githubUsername)
            it.claim(GITHUB_AVATAR_URL_CLAIM, githubAvatarUrl)
        }

    private fun generateToken(subject: String, expirationMillis: Long, claims: (JwtBuilder) -> Unit): String {
        val now = Instant.now()
        val builder = Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expirationMillis)))
            .signWith(secretKey)

        claims(builder)
        return builder.compact()
    }

    fun parseClaims(token: String): Claims =
        runCatching { parse(token) }
            .getOrElse { throw CoreException(ErrorType.INVALID_TOKEN) }

    fun getUserIdFromToken(token: String): Long = parseClaims(token).subject.toLong()

    fun getRoleFromToken(token: String): String =
        parseClaims(token).getOrNull(ROLE_CLAIM) ?: throw CoreException(ErrorType.INVALID_TOKEN)

    fun getTokenIdFromToken(token: String): String =
        parseClaims(token).getOrNull(TOKEN_ID_CLAIM) ?: throw CoreException(ErrorType.INVALID_TOKEN)

    fun getGitHubIdFromToken(token: String): String = parseClaims(token).subject

    fun getGitHubUsernameFromToken(token: String): String =
        parseClaims(token).getOrNull(GITHUB_USERNAME_CLAIM) ?: throw CoreException(ErrorType.INVALID_TOKEN)

    fun getGitHubAvatarUrlFromToken(token: String): String? = parseClaims(token).getOrNull(GITHUB_AVATAR_URL_CLAIM)

    fun validateToken(token: String): Boolean = runCatching { parseClaims(token) }.isSuccess

    private fun Claims.getOrNull(key: String): String? = try {
        get(key, String::class.java)
    } catch (_: Exception) {
        null
    }

    private fun parse(token: String): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
}