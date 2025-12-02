package com.blog.api.global.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "aws")
@ConditionalOnProperty(name = ["aws.access-key"], matchIfMissing = false)
data class AwsProperties(
    var region: String = "",
    var accessKey: String = "",
    var secretKey: String = "",
    var s3: S3Properties = S3Properties()
) {
    data class S3Properties(
        var bucket: String = ""
    )
}
