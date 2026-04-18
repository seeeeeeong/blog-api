package com.blog.api.core.support.security

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.JwtProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JwtProviderTest {
    private val jwtProvider =
        JwtProvider(
            jwtProperties =
                JwtProperties(
                    secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                    accessExpiration = 60_000L,
                    refreshExpiration = 120_000L,
                ),
        )

    @Test
    fun `USER_ACCESS 토큰은 user access 검증만 통과한다`() {
        val accessToken = jwtProvider.generateAccessToken(userId = 1L, role = "ADMIN")

        assertTrue(jwtProvider.validateUserAccessToken(accessToken))
    }

    @Test
    fun `USER_REFRESH 토큰만 refresh claim 파싱이 가능하다`() {
        val refreshToken = jwtProvider.generateRefreshToken(userId = 1L, role = "ADMIN")
        val accessToken = jwtProvider.generateAccessToken(userId = 1L, role = "ADMIN")

        val refreshClaims = jwtProvider.parseRefreshTokenClaims(refreshToken)
        val exception =
            assertThrows(CoreException::class.java) {
                jwtProvider.parseRefreshTokenClaims(accessToken)
            }

        assertEquals(1L, refreshClaims.userId)
        assertEquals("ADMIN", refreshClaims.roleName)
        assertEquals(ErrorType.INVALID_TOKEN, exception.errorType)
    }

    @Test
    fun `토큰 타입이 다르면 role 조회를 거부한다`() {
        val refreshToken = jwtProvider.generateRefreshToken(userId = 1L, role = "ADMIN")

        val exception =
            assertThrows(CoreException::class.java) {
                jwtProvider.getRoleFromToken(refreshToken)
            }
        assertEquals(ErrorType.INVALID_TOKEN, exception.errorType)
    }
}
