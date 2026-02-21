package com.blog.api.core.api.controller.v1.reqeust

data class ImagePresignedUrlCompleteRequest(
    val uploadToken: String,
    val key: String,
)
