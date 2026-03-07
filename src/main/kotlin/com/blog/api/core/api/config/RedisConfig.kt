package com.blog.api.core.api.config

import io.lettuce.core.ClientOptions
import io.lettuce.core.SocketOptions
import io.lettuce.core.TimeoutOptions
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
class RedisConfig {

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        return RedisTemplate<String, String>().apply {
            this.connectionFactory = connectionFactory
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
        }
    }

    /**
     * spring.data.redis.timeout 은 소켓 레벨 타임아웃만 설정한다.
     * Lettuce command-level timeout 은 ClientOptions.timeoutOptions() 로 별도 설정해야
     * Redis hang 시 설정값대로 즉시 타임아웃된다.
     */
    @Bean
    fun lettuceClientConfigurationCustomizer(): LettuceClientConfigurationBuilderCustomizer {
        return LettuceClientConfigurationBuilderCustomizer { builder ->
            builder
                .commandTimeout(Duration.ofMillis(250))
                .clientOptions(
                    ClientOptions.builder()
                        .socketOptions(
                            SocketOptions.builder()
                                .connectTimeout(Duration.ofMillis(500))
                                .build()
                        )
                        .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(250)))
                        .build()
                )
        }
    }
}