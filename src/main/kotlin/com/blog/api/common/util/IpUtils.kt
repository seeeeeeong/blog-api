package com.blog.api.common.util

import jakarta.servlet.http.HttpServletRequest

object IpUtils {

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
        for (header in IP_HEADERS) {
            val ip = request.getHeader(header)
            val validIp = extractValidIp(ip)
            if (validIp != null) {
                return validIp
            }
        }
        return request.remoteAddr
    }

    private fun extractValidIp(ip: String?): String? {
        if (ip.isNullOrBlank() || ip.equals(UNKNOWN, ignoreCase = true)) {
            return null
        }
        val firstIp = ip.split(IP_SEPARATOR)[0].trim()
        return firstIp.ifEmpty { null }
    }
}
