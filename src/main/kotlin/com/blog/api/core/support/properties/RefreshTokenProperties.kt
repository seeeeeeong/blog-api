package com.blog.api.core.support.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "refresh-token")
data class RefreshTokenProperties(

    val expirationSeconds: Long = 1_209_600,
    val gracePeriodSeconds: Long = 30,
    val maxFamiliesPerUser: Int = 10,
    val autoCleanupEnabled: Boolean = true,
    val cleanupIntervalSeconds: Long = 3_600
)
