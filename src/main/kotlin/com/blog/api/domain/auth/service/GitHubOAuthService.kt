package com.blog.api.domain.auth.service

import com.blog.api.domain.auth.dto.CommentAuthResponse
import com.blog.api.domain.auth.dto.GitHubTokenResponse
import com.blog.api.domain.auth.dto.GitHubUserResponse
import com.blog.api.global.exception.CustomException
import com.blog.api.global.exception.ErrorCode
import com.blog.api.global.security.JwtProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Service
class GitHubOAuthService(
    private val jwtProvider: JwtProvider,
    private val restTemplate: RestTemplate,
    @Value("\${spring.security.oauth2.client.registration.github.client-id}")
    private val clientId: String,
    @Value("\${spring.security.oauth2.client.registration.github.client-secret}")
    private val clientSecret: String
) {

    fun getAccessToken(code: String): String {
        val url = "https://github.com/login/oauth/access_token"
        
        val params = LinkedMultiValueMap<String, String>()
        params.add("client_id", clientId)
        params.add("client_secret", clientSecret)
        params.add("code", code)

        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)

        val request = HttpEntity(params, headers)
        
        val response = restTemplate.postForEntity(url, request, Map::class.java)
        
        val accessToken = response.body?.get("access_token") as? String
            ?: throw CustomException(ErrorCode.INVALID_TOKEN)
        
        return accessToken
    }

    fun getGitHubUser(accessToken: String): GitHubUserResponse {
        val url = "https://api.github.com/user"
        
        val headers = HttpHeaders()
        headers.setBearerAuth(accessToken)
        headers.accept = listOf(MediaType.APPLICATION_JSON)

        val request = HttpEntity<Void>(headers)
        
        val response = restTemplate.exchange(url, HttpMethod.GET, request, Map::class.java)
        val body = response.body ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        return GitHubUserResponse(
            id = (body["id"] as Number).toLong(),
            login = body["login"] as String,
            avatarUrl = body["avatar_url"] as? String,
            name = body["name"] as? String,
            email = body["email"] as? String
        )
    }

    fun generateCommentToken(githubUser: GitHubUserResponse): CommentAuthResponse {
        val commentToken = jwtProvider.generateAccessToken(githubUser.id)
        
        return CommentAuthResponse(
            commentToken = commentToken,
            githubId = githubUser.id.toString(),
            githubUsername = githubUser.login,
            githubAvatarUrl = githubUser.avatarUrl
        )
    }
}