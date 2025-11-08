package com.blog.api.global.controller

import com.blog.api.global.service.PresignedUrlResponse
import com.blog.api.global.service.S3Service
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
    ): ResponseEntity<PresignedUrlResponse> {
        val response = s3Service.generatePresignedUrl(fileName, contentType)
        return ResponseEntity.ok(response)
    }
}
