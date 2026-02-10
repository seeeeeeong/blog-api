package com.blog.api.core.support.connector

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class OAuthCodeStore(
    private val redisTemplate: RedisTemplate<String, String>
) {

    fun saveState(state: String, codeVerifier: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(stateKey(state), codeVerifier, ttlSeconds, TimeUnit.SECONDS)
    }

    fun consumeState(state: String): String? {
        val key = stateKey(state)
        val value = redisTemplate.opsForValue().get(key)
        if (value != null) {
            redisTemplate.delete(key)
        }
        return value
    }

    fun saveExchangeCode(code: String, payloadJson: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(exchangeKey(code), payloadJson, ttlSeconds, TimeUnit.SECONDS)
    }

    fun consumeExchangeCode(code: String): String? {
        val key = exchangeKey(code)
        val value = redisTemplate.opsForValue().get(key)
        if (value != null) {
            redisTemplate.delete(key)
        }
        return value
    }

    private fun stateKey(state: String) = "oauth:state:$state"

    private fun exchangeKey(code: String) = "oauth:exchange:$code"
}
