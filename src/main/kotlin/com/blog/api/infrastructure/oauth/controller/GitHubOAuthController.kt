package com.blog.api.infrastructure.oauth.controller

import com.blog.api.global.response.ApiResponse
import com.blog.api.global.security.JwtProvider
import com.blog.api.infrastructure.oauth.service.GitHubOAuthService
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/api/auth/github")
class GitHubOAuthController(
    private val gitHubOAuthService: GitHubOAuthService,
    private val jwtProvider: JwtProvider
) {

    @GetMapping("/callback")
    fun callback(@RequestParam code: String): RedirectView {
        val accessToken = gitHubOAuthService.getAccessToken(code)
        val githubUser = gitHubOAuthService.getGitHubUser(accessToken)

        val commentAuth = gitHubOAuthService.generateCommentToken(githubUser)

        val redirectUrl = "http://localhost:5173/auth/callback" +
                "?token=${commentAuth.commentToken}" +
                "&githubId=${commentAuth.githubId}" +
                "&githubUsername=${commentAuth.githubUsername}" +
                "&githubAvatarUrl=${commentAuth.githubAvatarUrl ?: ""}"

        return RedirectView(redirectUrl)
    }

    @GetMapping("/verify")
    fun verifyToken(@RequestHeader("Authorization") token: String): ApiResponse<Map<String, Boolean>> {
        val jwtToken = token.removePrefix("Bearer ")
        val isValid = jwtProvider.validateToken(jwtToken)
        return ApiResponse.success(mapOf("valid" to isValid))
    }
}