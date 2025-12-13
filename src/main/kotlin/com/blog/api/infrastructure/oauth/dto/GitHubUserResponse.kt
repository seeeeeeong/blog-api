package com.blog.api.infrastructure.oauth.dto

import com.blog.api.common.web.dto.GitHubUser

data class GitHubUserResponse(
    val id: Long,
    val login: String,
    val avatarUrl: String?,
    val name: String?,
    val email: String?
) {
    fun toGitHubUser(): GitHubUser {
        return GitHubUser(
            githubId = id.toString(),
            githubUsername = login,
            githubAvatarUrl = avatarUrl
        )
    }
}

data class CommentAuthResponse(
    val commentToken: String,
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String?
) {
    companion object {
        fun from(commentToken: String, githubUser: GitHubUser): CommentAuthResponse {
            return CommentAuthResponse(
                commentToken = commentToken,
                githubId = githubUser.githubId,
                githubUsername = githubUser.githubUsername,
                githubAvatarUrl = githubUser.githubAvatarUrl
            )
        }
    }
}