package com.blog.api.core.support.web

import jakarta.servlet.http.HttpServletRequest

object ClientIpResolver {

    private val IP_HEADERS = arrayOf(
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    )

    private const val UNKNOWN = "unknown"
    private const val IP_SEPARATOR = ","

    fun getClientIp(request: HttpServletRequest): String {
        return IP_HEADERS.asSequence()
            .mapNotNull { header -> request.getHeader(header)?.let { extractValidIp(it) } }
            .firstOrNull()
            ?: request.remoteAddr
    }

    private fun extractValidIp(ip: String): String? {
        val candidate = ip.split(IP_SEPARATOR).firstOrNull()?.trim()
        return candidate?.takeIf { it.isNotEmpty() && !it.equals(UNKNOWN, ignoreCase = true) }
    }
}
