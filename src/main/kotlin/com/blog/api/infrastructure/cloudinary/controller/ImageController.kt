package com.blog.api.infrastructure.cloudinary.controller

import com.blog.api.common.response.ApiResponse
import com.blog.api.infrastructure.cloudinary.service.CloudinaryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Image", description = "이미지 업로드 API")
@RestController
@RequestMapping("/api/images")
class ImageController(
    private val cloudinaryService: CloudinaryService
) {

    @Operation(
        summary = "이미지 업로드",
        description = "이미지를 Cloudinary에 업로드합니다."
    )
    @PostMapping("/upload")
    fun uploadImage(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(value = "folder", defaultValue = "blog") folder: String
    ) = ApiResponse.success(
        cloudinaryService.uploadImage(file, folder)
    )

    @Operation(
        summary = "업로드 서명 생성",
        description = "클라이언트에서 직접 업로드할 수 있는 서명을 생성합니다."
    )
    @GetMapping("/upload-signature")
    fun getUploadSignature(
        @RequestParam(value = "folder", defaultValue = "blog") folder: String
    ) = ApiResponse.success(
        cloudinaryService.generateUploadSignature(folder)
    )

    @Operation(
        summary = "이미지 삭제",
        description = "Cloudinary에서 이미지를 삭제합니다."
    )
    @DeleteMapping("/{publicId}")
    fun deleteImage(
        @PathVariable publicId: String
    ) = ApiResponse.success(
        mapOf("deleted" to cloudinaryService.deleteImage(publicId))
    )
}
