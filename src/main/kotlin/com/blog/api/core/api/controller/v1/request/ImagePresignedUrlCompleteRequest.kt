package com.blog.api.core.api.controller.v1.request

data class ImagePresignedUrlCompleteRequest(
    val uploadToken: String,
    val key: String,
)
