package com.blog.api.core.api.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "refresh-token")
data class RefreshTokenProperties(
    /**
     * Refresh token expiration in seconds
     * Default: 14 days (1,209,600 seconds)
     */
    val expirationSeconds: Long = 1_209_600,

    /**
     * Grace period for concurrent refresh requests in seconds
     * Allows recently used tokens to be accepted to handle race conditions
     * Default: 30 seconds
     */
    val gracePeriodSeconds: Long = 30,

    /**
     * Maximum number of active token families per user
     * Prevents unlimited device sessions
     * Default: 10
     */
    val maxFamiliesPerUser: Int = 10,

    /**
     * Enable automatic cleanup of expired tokens
     * Default: true
     */
    val autoCleanupEnabled: Boolean = true,

    /**
     * Cleanup interval in seconds
     * Default: 1 hour (3,600 seconds)
     */
    val cleanupIntervalSeconds: Long = 3_600
)
