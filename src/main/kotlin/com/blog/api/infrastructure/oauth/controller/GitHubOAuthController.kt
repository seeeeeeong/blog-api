package com.blog.api.infrastructure.oauth.controller

import com.blog.api.common.response.ApiResponse
import com.blog.api.infrastructure.oauth.dto.CommentAuthResponse
import com.blog.api.infrastructure.oauth.service.GitHubOAuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/api/auth/github")
class GitHubOAuthController(
    private val gitHubOAuthService: GitHubOAuthService,
    @Value("\${oauth.github.redirect-url}")
    private val redirectUrl: String
) {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val EMPTY_STRING = ""
        private const val QUERY_PARAM_TOKEN = "token"
        private const val QUERY_PARAM_GITHUB_ID = "githubId"
        private const val QUERY_PARAM_GITHUB_USERNAME = "githubUsername"
        private const val QUERY_PARAM_GITHUB_AVATAR_URL = "githubAvatarUrl"
        private const val RESPONSE_KEY_VALID = "valid"
    }

    @GetMapping("/callback")
    fun callback(@RequestParam code: String): ApiResponse<RedirectView> {
        val accessToken = gitHubOAuthService.getAccessToken(code)
        val githubUser = gitHubOAuthService.getGitHubUser(accessToken)
        val commentAuth = gitHubOAuthService.generateCommentToken(githubUser)

        val fullRedirectUrl = buildRedirectUrl(commentAuth)

        return ApiResponse.success(RedirectView(fullRedirectUrl))
    }

    @GetMapping("/verify")
    fun verifyToken(@RequestHeader("Authorization") authorization: String): ApiResponse<Map<String, Boolean>> {
        val jwtToken = extractBearerToken(authorization)
        val isValid = gitHubOAuthService.verifyToken(jwtToken)
        return ApiResponse.success(mapOf(RESPONSE_KEY_VALID to isValid))
    }

    private fun buildRedirectUrl(commentAuth: CommentAuthResponse): String {
        val avatarUrl = commentAuth.githubAvatarUrl ?: EMPTY_STRING
        return "$redirectUrl?" +
                "$QUERY_PARAM_TOKEN=${commentAuth.commentToken}&" +
                "$QUERY_PARAM_GITHUB_ID=${commentAuth.githubId}&" +
                "$QUERY_PARAM_GITHUB_USERNAME=${commentAuth.githubUsername}&" +
                "$QUERY_PARAM_GITHUB_AVATAR_URL=$avatarUrl"
    }

    private fun extractBearerToken(authorization: String): String {
        return authorization.removePrefix(BEARER_PREFIX)
    }
}
