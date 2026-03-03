package com.blog.api.core.support.security

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JwtProviderTest {

    private val jwtProvider = JwtProvider(
        secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        accessExpiration = 60_000L,
        refreshExpiration = 120_000L,
    )

    @Test
    fun `USER_ACCESS 토큰은 user access 검증만 통과한다`() {
        val accessToken = jwtProvider.generateAccessToken(userId = 1L, role = "ADMIN")

        assertTrue(jwtProvider.validateUserAccessToken(accessToken))
        assertFalse(jwtProvider.validateOAuthCommentToken(accessToken))
    }

    @Test
    fun `USER_REFRESH 토큰만 refresh claim 파싱이 가능하다`() {
        val refreshToken = jwtProvider.generateRefreshToken(
            userId = 1L,
            tokenId = "token-id-1",
            role = "ADMIN",
        )
        val accessToken = jwtProvider.generateAccessToken(userId = 1L, role = "ADMIN")

        val refreshClaims = jwtProvider.parseRefreshTokenClaims(refreshToken)
        val exception = assertThrows(CoreException::class.java) {
            jwtProvider.parseRefreshTokenClaims(accessToken)
        }

        assertEquals("token-id-1", refreshClaims.tokenId)
        assertEquals(1L, refreshClaims.userId)
        assertEquals("ADMIN", refreshClaims.roleName)
        assertEquals(ErrorType.INVALID_TOKEN, exception.errorType)
    }

    @Test
    fun `OAUTH_COMMENT 토큰은 OAuth 검증만 통과한다`() {
        val oauthToken = jwtProvider.generateOAuthAccessToken(
            oauthId = 1001L,
            oauthUsername = "octocat",
            oauthAvatarUrl = "https://avatars.githubusercontent.com/u/1?v=4",
        )

        assertTrue(jwtProvider.validateOAuthCommentToken(oauthToken))
        assertFalse(jwtProvider.validateUserAccessToken(oauthToken))
    }

    @Test
    fun `토큰 타입이 다르면 role 조회를 거부한다`() {
        val oauthToken = jwtProvider.generateOAuthAccessToken(
            oauthId = 1001L,
            oauthUsername = "octocat",
            oauthAvatarUrl = null,
        )

        val exception = assertThrows(CoreException::class.java) {
            jwtProvider.getRoleFromToken(oauthToken)
        }
        assertEquals(ErrorType.INVALID_TOKEN, exception.errorType)
    }
}
