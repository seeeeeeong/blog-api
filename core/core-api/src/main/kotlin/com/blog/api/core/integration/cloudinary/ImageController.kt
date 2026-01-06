package com.blog.api.core.integration.cloudinary

import com.blog.api.core.integration.cloudinary.CloudinaryService
import com.blog.api.core.support.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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
    ) = ApiResponse.Companion.success(
        ImageUploadResponse.Companion.of(cloudinaryService.uploadImage(file.bytes, folder))
    )

    @Operation(
        summary = "업로드 서명 생성",
        description = "클라이언트에서 직접 업로드할 수 있는 서명을 생성합니다."
    )
    @GetMapping("/upload-signature")
    fun getUploadSignature(
        @RequestParam(value = "folder", defaultValue = "blog") folder: String
    ) = ApiResponse.Companion.success(
        ImageSignatureResponse.Companion.of(cloudinaryService.generateUploadSignature(folder))
    )

    @Operation(
        summary = "이미지 삭제",
        description = "Cloudinary에서 이미지를 삭제합니다."
    )
    @DeleteMapping("/{publicId}")
    fun deleteImage(
        @PathVariable publicId: String
    ) = ApiResponse.Companion.success(
        mapOf("deleted" to cloudinaryService.deleteImage(publicId))
    )
}