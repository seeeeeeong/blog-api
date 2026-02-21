package com.blog.api.core.api.controller.v1

import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.api.controller.v1.reqeust.OAuthLoginRequest
import com.blog.api.core.api.controller.v1.response.OAuthLoginResponse
import com.blog.api.core.domain.OAuthService
import com.blog.api.core.support.properties.OAuthUserProperties
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

    @GetMapping("/authorize")
    fun authorize(): ApiResponse<String> {
        return ApiResponse.success(oauthService.createAuthorizationUrl())
    }

    @GetMapping("/callback")
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String
    ): RedirectView {
        val exchangeCode = oauthService.handleCallback(code, state)
        return RedirectView(
            UriComponentsBuilder.fromUriString(properties.redirectUrl)
                .queryParam("code", exchangeCode)
                .build(true)
                .toUriString()
        )
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
}
