package com.blog.api.core.support.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

@Component
class ClientFingerprintArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(ClientFingerprint::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): String {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw IllegalStateException("HttpServletRequest not found")

        return hashSeed(buildSeed(request))
    }

    private fun buildSeed(request: HttpServletRequest): String {
        val userId = SecurityContextHolder.getContext().authentication?.principal as? Long
        if (userId != null) {
            return "user:$userId"
        }

        val ip = ClientIpResolver.getClientIp(request)
        val userAgent = request.getHeader(USER_AGENT_HEADER) ?: UNKNOWN
        return "ip:$ip|ua:$userAgent"
    }

    private fun hashSeed(seed: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            .digest(seed.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    companion object {
        private const val USER_AGENT_HEADER = "User-Agent"
        private const val UNKNOWN = "unknown"
        private const val HASH_ALGORITHM = "SHA-256"
    }
}
