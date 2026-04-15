package com.blog.api.core.api.controller.v1

import com.blog.api.core.api.controller.v1.response.ImagePresignedUrlResponse
import com.blog.api.core.domain.image.ImageService
import com.blog.api.core.support.auth.Admin
import com.blog.api.core.support.response.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/images")
class ImageController(
    private val imageService: ImageService,
) {

    @GetMapping("/presigned-url")
    fun getPresignedUrl(
        @Admin userId: Long,
        @RequestParam(value = "contentType", defaultValue = "image/png") contentType: String,
        @RequestParam(value = "folder", defaultValue = "blog") folder: String,
    ): ApiResponse<ImagePresignedUrlResponse> {
        val presignedUrl = imageService.generatePresignedUrl(userId, contentType, folder)
        return ApiResponse.success(ImagePresignedUrlResponse.of(presignedUrl))
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteImage(
        @Admin userId: Long,
        @RequestParam key: String,
    ) {
        imageService.deleteImage(userId, key)
    }
}
