package com.blog.api.common.web.dto

data class GitHubUser(
    val githubId: String,
    val githubUsername: String,
    val githubAvatarUrl: String?
)
