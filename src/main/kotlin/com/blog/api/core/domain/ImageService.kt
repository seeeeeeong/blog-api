package com.blog.api.core.domain

import com.blog.api.core.support.properties.CloudinaryProperties
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.springframework.stereotype.Service

@Service
class ImageService(
    private val cloudinary: Cloudinary,
    private val cloudinaryProperties: CloudinaryProperties,
) {

    fun uploadImage(bytes: ByteArray, folder: String? = null): ImageUpload {
        val targetFolder = folder ?: cloudinaryProperties.defaultFolder
        val options = ObjectUtils.asMap(
            "folder", targetFolder,
            "resource_type", "auto",
        )

        val uploadResult = cloudinary.uploader().upload(bytes, options)

        return ImageUpload(
            url = uploadResult["secure_url"] as String,
            publicId = uploadResult["public_id"] as String,
            format = uploadResult["format"] as String,
            width = uploadResult["width"] as Int,
            height = uploadResult["height"] as Int,
        )
    }

    fun deleteImage(publicId: String): Boolean {
        val result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
        return result["result"] == "ok"
    }

    fun generateUploadSignature(folder: String? = null): ImageSignature {
        val targetFolder = folder ?: cloudinaryProperties.defaultFolder
        val timestamp = System.currentTimeMillis() / 1000
        val params = mapOf(
            "timestamp" to timestamp,
            "folder" to targetFolder,
        )

        val signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret)

        return ImageSignature(
            signature = signature,
            timestamp = timestamp,
            apiKey = cloudinary.config.apiKey,
            cloudName = cloudinary.config.cloudName,
        )
    }
}
