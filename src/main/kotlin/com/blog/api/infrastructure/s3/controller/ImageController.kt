package com.blog.api.infrastructure.s3.controller

import com.blog.api.infrastructure.s3.service.S3Service
import org.springframework.http.ResponseEntity
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
    ): ResponseEntity<com.blog.api.infrastructure.s3.service.PresignedUrlResponse?> {
        val response = s3Service.generatePresignedUrl(fileName, contentType)
        return ResponseEntity.ok(response)
    }
}