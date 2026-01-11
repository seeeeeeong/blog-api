package com.blog.api.core.api.controller.v1

import com.blog.api.core.support.response.ApiResponse
import com.blog.api.core.domain.OAuthUser
import com.blog.api.core.domain.OAuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/api/auth/github")
class OAuthController(
    private val oauthService: OAuthService,
    @param:Value("\${oauth.github.redirect-url}")
    private val redirectUrl: String
) {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val QUERY_PARAM_TOKEN = "token"
        private const val QUERY_PARAM_OAUTH_ID = "oauthId"
        private const val QUERY_PARAM_OAUTH_USERNAME = "oauthUsername"
        private const val QUERY_PARAM_OAUTH_AVATAR_URL = "oauthAvatarUrl"
        private const val RESPONSE_KEY_VALID = "valid"
    }

    @GetMapping("/callback")
    fun callback(@RequestParam code: String): RedirectView {
        val accessToken = oauthService.getAccessToken(code)
        val oauthUser = oauthService.getOAuthUser(accessToken)
        val commentToken = oauthService.generateCommentToken(oauthUser)

        val fullRedirectUrl = buildRedirectUrl(commentToken, oauthUser)

        return RedirectView(fullRedirectUrl)
    }

    @GetMapping("/verify")
    fun verifyToken(@RequestHeader("Authorization") authorization: String): ApiResponse<Map<String, Boolean>> {
        val jwtToken = extractBearerToken(authorization)
        val isValid = oauthService.verifyToken(jwtToken)
        return ApiResponse.Companion.success(mapOf(RESPONSE_KEY_VALID to isValid))
    }

    private fun buildRedirectUrl(commentToken: String, oauthUser: OAuthUser): String {
        return "$redirectUrl?" +
                "$QUERY_PARAM_TOKEN=$commentToken&" +
                "$QUERY_PARAM_OAUTH_ID=${oauthUser.id}&" +
                "$QUERY_PARAM_OAUTH_USERNAME=${oauthUser.login}&" +
                "$QUERY_PARAM_OAUTH_AVATAR_URL=${oauthUser.avatarUrl}"
    }

    private fun extractBearerToken(authorization: String): String {
        return authorization.removePrefix(BEARER_PREFIX)
    }
}