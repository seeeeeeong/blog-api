package com.blog.api.infrastructure.oauth.dto

data class GitHubUser(
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String?
)

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
