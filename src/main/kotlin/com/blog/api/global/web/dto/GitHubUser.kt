package com.blog.api.global.web.dto

data class GitHubUser(
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String?
)
