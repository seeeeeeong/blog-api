package com.blog.api.core.domain

import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.oauth.GithubOAuthClient
import com.blog.api.core.support.oauth.GithubUserResponse
import com.blog.api.core.support.properties.OAuthUserProperties
import com.blog.api.core.support.security.JwtProvider
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.concurrent.TimeUnit

class OAuthServiceTest {

    @Test
    fun `state 쿠키가 다르면 callback 을 거부한다`() {
        val fixture = fixture()

        val exception = assertThrows(CoreException::class.java) {
            fixture.service.handleCallback(code = "github-code", state = "state-1", stateCookie = "state-2")
        }

        assertEquals(ErrorType.OAUTH_STATE_INVALID, exception.errorType)
        verify(fixture.valueOps, never()).getAndDelete(anyString())
        verify(fixture.githubOAuthClient, never()).fetchAccessToken(anyString())
    }

    @Test
    fun `callback 성공 시 state 는 소비되고 exchange code 가 저장된다`() {
        val fixture = fixture()
        Mockito.`when`(fixture.valueOps.getAndDelete("oauth:state:state-123")).thenReturn("1")
        Mockito.`when`(fixture.githubOAuthClient.fetchAccessToken("github-code")).thenReturn("github-access-token")
        Mockito.`when`(fixture.githubOAuthClient.fetchUser("github-access-token")).thenReturn(
            GithubUserResponse(
                id = 1001L,
                login = "octocat",
                avatarUrl = "https://avatars.githubusercontent.com/u/1?v=4",
                name = "Octo Cat",
                email = "octocat@example.com",
            )
        )
        Mockito.`when`(
            fixture.jwtProvider.generateOAuthAccessToken(
                1001L,
                "octocat",
                "https://avatars.githubusercontent.com/u/1?v=4",
            )
        ).thenReturn("oauth-comment-token")

        val exchangeCode = fixture.service.handleCallback(
            code = "github-code",
            state = "state-123",
            stateCookie = "state-123",
        )

        assertTrue(exchangeCode.isNotBlank())
        verify(fixture.valueOps).getAndDelete("oauth:state:state-123")
        val exchangeKeyCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(fixture.valueOps).set(
            exchangeKeyCaptor.capture(),
            anyString(),
            eq(120L),
            eq(TimeUnit.SECONDS),
        )
        assertTrue(exchangeKeyCaptor.value.startsWith("oauth:exchange:"))
    }

    @Test
    fun `exchangeCode 는 1회만 사용 가능하다`() {
        val fixture = fixture()
        val payload = fixture.objectMapper.writeValueAsString(
            OAuthLogin(
                token = "oauth-comment-token",
                user = OAuthUser(
                    id = 1001L,
                    login = "octocat",
                    avatarUrl = "https://avatars.githubusercontent.com/u/1?v=4",
                    name = "Octo Cat",
                    email = "octocat@example.com",
                ),
            )
        )
        Mockito.`when`(fixture.valueOps.getAndDelete("oauth:exchange:exchange-1"))
            .thenReturn(payload)
            .thenReturn(null)

        val first = fixture.service.exchangeCode("exchange-1")
        val secondException = assertThrows(CoreException::class.java) {
            fixture.service.exchangeCode("exchange-1")
        }

        assertEquals("oauth-comment-token", first.token)
        assertNotNull(first.user)
        assertEquals(ErrorType.OAUTH_CODE_INVALID, secondException.errorType)
    }

    @Test
    fun `verifyToken 은 OAuth 댓글 토큰만 허용한다`() {
        val fixture = fixture()
        Mockito.`when`(fixture.jwtProvider.validateOAuthCommentToken("oauth-token")).thenReturn(true)
        Mockito.`when`(fixture.jwtProvider.validateOAuthCommentToken("user-token")).thenReturn(false)

        assertTrue(fixture.service.verifyToken("oauth-token"))
        assertFalse(fixture.service.verifyToken("user-token"))
        verify(fixture.jwtProvider).validateOAuthCommentToken("oauth-token")
        verify(fixture.jwtProvider).validateOAuthCommentToken("user-token")
    }

    private fun fixture(): Fixture {
        val jwtProvider = mock(JwtProvider::class.java)
        val githubOAuthClient = mock(GithubOAuthClient::class.java)
        @Suppress("UNCHECKED_CAST")
        val redisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, String>
        @Suppress("UNCHECKED_CAST")
        val valueOps = mock(ValueOperations::class.java) as ValueOperations<String, String>
        Mockito.`when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        val objectMapper = jacksonObjectMapper()
        val properties = OAuthUserProperties(
            clientId = "client-id",
            clientSecret = "client-secret",
            callbackUrl = "http://localhost:8080/api/auth/github/callback",
            redirectUrl = "http://localhost:5173/auth/callback",
        )
        val service = OAuthService(jwtProvider, githubOAuthClient, redisTemplate, objectMapper, properties)
        return Fixture(service, jwtProvider, githubOAuthClient, valueOps, objectMapper)
    }

    private data class Fixture(
        val service: OAuthService,
        val jwtProvider: JwtProvider,
        val githubOAuthClient: GithubOAuthClient,
        val valueOps: ValueOperations<String, String>,
        val objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
    )
}
