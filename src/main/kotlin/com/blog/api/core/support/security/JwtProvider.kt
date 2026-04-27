package com.blog.api.core.support.security

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties,
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(Charsets.UTF_8))

    companion object {
        private const val ROLE_CLAIM = "role"
        private const val TOKEN_TYPE_CLAIM = "tokenType"
        private const val TOKEN_TYPE_USER_ACCESS = "USER_ACCESS"
        private const val TOKEN_TYPE_USER_REFRESH = "USER_REFRESH"
    }

    fun generateAccessToken(
        userId: Long,
        role: String,
    ): String =
        generateToken(userId.toString(), jwtProperties.accessExpiration) {
            it.claim(ROLE_CLAIM, role)
            it.claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_USER_ACCESS)
        }

    fun generateRefreshToken(
        userId: Long,
        role: String,
    ): String =
        generateToken(userId.toString(), jwtProperties.refreshExpiration) {
            it.claim(ROLE_CLAIM, role)
            it.claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_USER_REFRESH)
        }

    fun getUserIdFromToken(token: String): Long {
        val claims = parseClaims(token)
        requireTokenType(claims, TOKEN_TYPE_USER_ACCESS)
        return claims.subject.toLong()
    }

    fun getRoleFromToken(token: String): String {
        val claims = parseClaims(token)
        requireTokenType(claims, TOKEN_TYPE_USER_ACCESS)
        return claims.getOrNull(ROLE_CLAIM) ?: throw CoreException(ErrorType.INVALID_TOKEN)
    }

    fun parseRefreshTokenClaims(token: String): RefreshTokenClaims {
        val claims = parseClaims(token)
        requireTokenType(claims, TOKEN_TYPE_USER_REFRESH)
        return RefreshTokenClaims(
            userId = claims.subject.toLong(),
            roleName = claims.getOrNull(ROLE_CLAIM) ?: throw CoreException(ErrorType.INVALID_TOKEN),
        )
    }

    fun validateUserAccessToken(token: String): Boolean =
        try {
            val claims = parseClaims(token)
            claims.getOrNull(TOKEN_TYPE_CLAIM) == TOKEN_TYPE_USER_ACCESS
        } catch (_: CoreException) {
            false
        }

    private fun generateToken(
        subject: String,
        expirationMillis: Long,
        claims: (JwtBuilder) -> Unit,
    ): String {
        val now = Instant.now()
        val builder =
            Jwts
                .builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(secretKey)

        claims(builder)
        return builder.compact()
    }

    private fun parseClaims(token: String): Claims =
        try {
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            throw CoreException(ErrorType.EXPIRED_TOKEN, e)
        } catch (e: Exception) {
            throw CoreException(ErrorType.INVALID_TOKEN, e)
        }

    private fun Claims.getOrNull(key: String): String? =
        try {
            get(key, String::class.java)
        } catch (_: Exception) {
            null
        }

    private fun requireTokenType(
        claims: Claims,
        expectedTokenType: String,
    ) {
        if (claims.getOrNull(TOKEN_TYPE_CLAIM) != expectedTokenType) {
            throw CoreException(ErrorType.INVALID_TOKEN)
        }
    }
}
