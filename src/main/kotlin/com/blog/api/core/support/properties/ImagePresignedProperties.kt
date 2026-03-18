package com.blog.api.core.support.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "image-upload.presigned")
data class ImagePresignedProperties(
    val ttlSeconds: Long = 180,
    val oneTimeTokenTtlSeconds: Long = 300,
    val allowedContentTypes: List<String> = listOf("image/jpeg", "image/png", "image/webp", "image/gif", "image/tiff"),
    val allowedFolders: List<String> = listOf("blog"),
)
