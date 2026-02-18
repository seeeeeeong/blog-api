package com.blog.api.core.support.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aws.s3")
data class S3Properties(
    val bucket: String,
    val region: String,
    val cloudfrontDomain: String = "",
    val defaultFolder: String = "blog"
)
