package com.blog.api.common.util

import jakarta.servlet.http.HttpServletRequest

object IpUtils {

    fun getClientIp(request: HttpServletRequest): String {
        val headers = listOf(
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

        headers.forEach { header ->
            val ip = request.getHeader(header)?.takeIf {
                it.isNotBlank() && !it.equals("unknown", ignoreCase = true)
            }

            ip?.let { return it.split(",")[0].trim() }
        }

        return request.remoteAddr
    }
}