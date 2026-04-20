package com.blog.api.core.api.controller.v1

import com.blog.api.core.domain.backfill.BackfillProxyService
import com.blog.api.core.support.auth.Admin
import com.blog.api.core.support.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/backfill")
class BackfillController(
    private val backfillProxyService: BackfillProxyService,
) {
    @PostMapping("/content")
    fun backfillContent(
        @Admin userId: Long,
        @RequestParam(defaultValue = "50") batchSize: Int,
    ): ApiResponse<Map<String, Int>> {
        val filled = backfillProxyService.backfillContent(batchSize)
        return ApiResponse.success(mapOf("filled" to filled))
    }

    @PostMapping("/embedding")
    fun backfillEmbedding(
        @Admin userId: Long,
    ): ApiResponse<Map<String, Int>> {
        val processed = backfillProxyService.backfillEmbedding()
        return ApiResponse.success(mapOf("processed" to processed))
    }

    @GetMapping("/stats")
    fun getStats(
        @Admin userId: Long,
    ): ApiResponse<Map<String, Any>> {
        val stats = backfillProxyService.getStats()
        return ApiResponse.success(stats)
    }
}
