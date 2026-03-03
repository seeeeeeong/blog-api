package com.blog.api.core.api.controller.v1

import com.blog.api.core.domain.OAuthAuthorization
import com.blog.api.core.domain.OAuthService
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.core.support.properties.OAuthUserProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class OAuthControllerTest {

    @Test
    fun `authorize 는 state 쿠키를 내려준다`() {
        val oauthService = mock(OAuthService::class.java)
        val controller = OAuthController(oauthService, properties())
        Mockito.`when`(oauthService.createAuthorization()).thenReturn(
            OAuthAuthorization(
                url = "https://github.com/login/oauth/authorize?state=state-123",
                state = "state-123",
                stateTtlSeconds = 600L,
            )
        )
        val request = MockHttpServletRequest().apply {
            addHeader("X-Forwarded-Proto", "https")
        }
        val response = MockHttpServletResponse()

        val apiResponse = controller.authorize(request, response)
        val cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE)

        assertEquals("https://github.com/login/oauth/authorize?state=state-123", apiResponse.data)
        assertNotNull(cookieHeader)
        assertTrue(cookieHeader!!.contains("oauth_state=state-123"))
        assertTrue(cookieHeader.contains("Path=/api/auth/github/callback"))
        assertTrue(cookieHeader.contains("HttpOnly"))
        assertTrue(cookieHeader.contains("SameSite=Lax"))
        assertTrue(cookieHeader.contains("Secure"))
    }

    @Test
    fun `callback 성공 시 redirect 하고 state 쿠키를 제거한다`() {
        val oauthService = mock(OAuthService::class.java)
        val controller = OAuthController(oauthService, properties())
        Mockito.`when`(oauthService.handleCallback("github-code", "state-123", "state-123"))
            .thenReturn("exchange-123")
        val request = MockHttpServletRequest("GET", "/api/auth/github/callback")
        val response = MockHttpServletResponse()

        val redirectView = controller.callback(
            code = "github-code",
            state = "state-123",
            stateCookie = "state-123",
            request = request,
            response = response,
        )
        val cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE)

        assertEquals("http://localhost:5173/auth/callback?code=exchange-123", redirectView.url)
        assertNotNull(cookieHeader)
        assertTrue(cookieHeader!!.contains("oauth_state="))
        assertTrue(cookieHeader.contains("Max-Age=0"))
        verify(oauthService).handleCallback("github-code", "state-123", "state-123")
    }

    @Test
    fun `callback 실패 시에도 state 쿠키를 제거한다`() {
        val oauthService = mock(OAuthService::class.java)
        val controller = OAuthController(oauthService, properties())
        Mockito.`when`(oauthService.handleCallback("github-code", "state-123", "bad-cookie"))
            .thenThrow(CoreException(ErrorType.OAUTH_STATE_INVALID))
        val request = MockHttpServletRequest("GET", "/api/auth/github/callback")
        val response = MockHttpServletResponse()

        assertThrows(CoreException::class.java) {
            controller.callback(
                code = "github-code",
                state = "state-123",
                stateCookie = "bad-cookie",
                request = request,
                response = response,
            )
        }
        val cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE)
        assertNotNull(cookieHeader)
        assertTrue(cookieHeader!!.contains("oauth_state="))
        assertTrue(cookieHeader.contains("Max-Age=0"))
    }

    private fun properties(): OAuthUserProperties = OAuthUserProperties(
        clientId = "client-id",
        clientSecret = "client-secret",
        callbackUrl = "http://localhost:8080/api/auth/github/callback",
        redirectUrl = "http://localhost:5173/auth/callback",
    )
}
