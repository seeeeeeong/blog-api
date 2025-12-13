package com.blog.api.infrastructure.oauth.controller

import com.blog.api.common.response.ApiResponse
import com.blog.api.common.security.JwtProvider
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
    private val jwtProvider: JwtProvider,
    @Value("\${oauth.github.redirect-url}")
    private val redirectUrl: String
) {

    @GetMapping("/callback")
    fun callback(@RequestParam code: String): ApiResponse<RedirectView> {
        val accessToken = gitHubOAuthService.getAccessToken(code)
        val githubUser = gitHubOAuthService.getGitHubUser(accessToken)
        val commentAuth = gitHubOAuthService.generateCommentToken(githubUser)

        val avatarUrlParam = if (commentAuth.githubAvatarUrl != null) {
            commentAuth.githubAvatarUrl
        } else {
            ""
        }

        val fullRedirectUrl = "$redirectUrl" +
                "?token=${commentAuth.commentToken}" +
                "&githubId=${commentAuth.githubId}" +
                "&githubUsername=${commentAuth.githubUsername}" +
                "&githubAvatarUrl=$avatarUrlParam"

        return ApiResponse.success(RedirectView(fullRedirectUrl))
    }

    @GetMapping("/verify")
    fun verifyToken(@RequestHeader("Authorization") token: String): ApiResponse<Map<String, Boolean>> {
        val jwtToken = token.removePrefix("Bearer ")
        val isValid = jwtProvider.validateToken(jwtToken)
        return ApiResponse.success(mapOf("valid" to isValid))
    }
}
