package com.blog.api.infrastructure.s3.controller

import com.blog.api.global.response.ApiResponse
import com.blog.api.infrastructure.s3.dto.PresignedUrlResponse
import com.blog.api.infrastructure.s3.service.S3Service
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/images")
class ImageController(
    private val s3Service: S3Service
) {

    @GetMapping("/presigned-url")
    fun getPresignedUrl(
        @RequestParam fileName: String,
        @RequestParam contentType: String
    ) = ApiResponse.success(
        s3Service.generatePresignedUrl(fileName, contentType)
    )
}