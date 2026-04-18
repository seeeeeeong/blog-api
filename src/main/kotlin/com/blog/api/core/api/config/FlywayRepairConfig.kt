package com.blog.api.core.api.config

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("prod")
class FlywayRepairConfig {
    private val log = LoggerFactory.getLogger(FlywayRepairConfig::class.java)

    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy =
        FlywayMigrationStrategy { flyway: Flyway ->
            log.info("Running Flyway repair before migrate to fix checksum mismatches")
            flyway.repair()
            flyway.migrate()
        }
}
