package com.blog.api.infrastructure.cloudinary.service

import com.blog.api.infrastructure.cloudinary.dto.CloudinarySignatureResponse
import com.blog.api.infrastructure.cloudinary.dto.CloudinaryUploadResponse
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class CloudinaryService(
    private val cloudinary: Cloudinary
) {

    fun uploadImage(file: MultipartFile, folder: String = "blog"): CloudinaryUploadResponse {
        val uploadResult = cloudinary.uploader().upload(
            file.bytes,
            ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "auto"
            )
        )

        return CloudinaryUploadResponse(
            url = uploadResult["secure_url"] as String,
            publicId = uploadResult["public_id"] as String,
            format = uploadResult["format"] as String,
            width = uploadResult["width"] as Int,
            height = uploadResult["height"] as Int
        )
    }

    fun deleteImage(publicId: String): Boolean {
        val result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
        return result["result"] == "ok"
    }

    fun generateUploadSignature(folder: String = "blog"): CloudinarySignatureResponse {
        val timestamp = System.currentTimeMillis() / 1000
        val params = mapOf(
            "timestamp" to timestamp,
            "folder" to folder
        )

        val signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret)

        return CloudinarySignatureResponse(
            signature = signature,
            timestamp = timestamp,
            apiKey = cloudinary.config.apiKey,
            cloudName = cloudinary.config.cloudName
        )
    }
}
