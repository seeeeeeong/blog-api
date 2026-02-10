package com.blog.api.core.api.controller.v1

import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.api.controller.v1.reqeust.OAuthExchangeRequest
import com.blog.api.core.api.controller.v1.response.OAuthAuthorizeResponse
import com.blog.api.core.api.controller.v1.response.OAuthExchangeResponse
import com.blog.api.core.domain.OAuthService
import org.springframework.beans.factory.annotation.Value
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
    @param:Value("\${oauth.github.redirect-url}")
    private val redirectUrl: String
) {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val RESPONSE_KEY_VALID = "valid"
    }

    @GetMapping("/authorize")
    fun authorize(): ApiResponse<OAuthAuthorizeResponse> {
        val authorizeUrl = oauthService.createAuthorizationUrl()
        return ApiResponse.Companion.success(OAuthAuthorizeResponse(authorizeUrl))
    }

    @GetMapping("/callback")
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String
    ): RedirectView {
        val exchangeCode = oauthService.handleCallback(code, state)
        return RedirectView(buildRedirectUrl(exchangeCode))
    }

    @PostMapping("/exchange")
    fun exchange(@RequestBody request: OAuthExchangeRequest): ApiResponse<OAuthExchangeResponse> {
        val result = oauthService.exchangeCode(request.toCode())
        return ApiResponse.Companion.success(OAuthExchangeResponse.of(result))
    }

    @GetMapping("/verify")
    fun verifyToken(@RequestHeader("Authorization") authorization: String): ApiResponse<Map<String, Boolean>> {
        val jwtToken = extractBearerToken(authorization)
        val isValid = oauthService.verifyToken(jwtToken)
        return ApiResponse.Companion.success(mapOf(RESPONSE_KEY_VALID to isValid))
    }

    private fun buildRedirectUrl(exchangeCode: String): String {
        return UriComponentsBuilder.fromUriString(redirectUrl)
            .queryParam("code", exchangeCode)
            .build(true)
            .toUriString()
    }

    private fun extractBearerToken(authorization: String): String {
        return authorization.removePrefix(BEARER_PREFIX)
    }
}
