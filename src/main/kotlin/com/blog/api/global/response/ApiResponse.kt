package com.blog.api.global.response

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data)
        }

        fun success(): ApiResponse<Unit> {
            return ApiResponse(success = true)
        }

        fun <T> success(data: T, message: String): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }

        fun <T> error(message: String): ApiResponse<T> {
            return ApiResponse(success = false, message = message)
        }
    }
}