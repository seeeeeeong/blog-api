package com.blog.api.core.api.controller.v1

import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.api.controller.v1.request.OAuthLoginRequest
import com.blog.api.core.api.controller.v1.response.OAuthLoginResponse
import com.blog.api.core.domain.OAuthService
import com.blog.api.core.support.properties.OAuthUserProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.util.UriComponentsBuilder

@RestController
@RequestMapping("/api/auth/github")
class OAuthController(
    private val oauthService: OAuthService,
    private val properties: OAuthUserProperties,
) {
    companion object {
        private const val STATE_COOKIE_NAME = "oauth_state"
        private const val CALLBACK_PATH = "/api/auth/github/callback"
        private const val HTTPS_SCHEME = "https"
    }

    @GetMapping("/authorize")
    fun authorize(request: HttpServletRequest, response: HttpServletResponse): ApiResponse<String> {
        val authorization = oauthService.createAuthorization()
        addStateCookie(
            response = response,
            request = request,
            value = authorization.state,
            maxAgeSeconds = authorization.stateTtlSeconds,
        )
        return ApiResponse.success(authorization.url)
    }

    @GetMapping("/callback")
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String,
        @CookieValue(name = STATE_COOKIE_NAME, required = false) stateCookie: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): RedirectView {
        return try {
            val exchangeCode = oauthService.handleCallback(code, state, stateCookie)
            RedirectView(
                UriComponentsBuilder.fromUriString(properties.redirectUrl)
                    .queryParam("code", exchangeCode)
                    .build(true)
                    .toUriString()
            )
        } finally {
            addStateCookie(
                response = response,
                request = request,
                value = "",
                maxAgeSeconds = 0L,
            )
        }
    }

    @PostMapping("/exchange")
    fun exchange(@RequestBody request: OAuthLoginRequest): ApiResponse<OAuthLoginResponse> {
        return ApiResponse.success(OAuthLoginResponse.of(oauthService.exchangeCode(request.code)))
    }

    @GetMapping("/verify")
    fun verifyToken(@RequestHeader("Authorization") authorization: String): ApiResponse<Map<String, Boolean>> {
        val isValid = oauthService.verifyToken(authorization.removePrefix("Bearer "))
        return ApiResponse.success(mapOf("valid" to isValid))
    }

    private fun addStateCookie(
        response: HttpServletResponse,
        request: HttpServletRequest,
        value: String,
        maxAgeSeconds: Long,
    ) {
        val stateCookie = ResponseCookie.from(STATE_COOKIE_NAME, value)
            .httpOnly(true)
            .secure(isSecureRequest(request))
            .sameSite("Lax")
            .path(CALLBACK_PATH)
            .maxAge(maxAgeSeconds)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString())
    }

    private fun isSecureRequest(request: HttpServletRequest): Boolean {
        if (request.isSecure) return true
        return request.getHeader("X-Forwarded-Proto")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.equals(HTTPS_SCHEME, ignoreCase = true)
            ?: false
    }
}
