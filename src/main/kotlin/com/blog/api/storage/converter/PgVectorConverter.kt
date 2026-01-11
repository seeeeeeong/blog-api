package com.blog.api.storage.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class PgVectorConverter : AttributeConverter<FloatArray?, String?> {

    override fun convertToDatabaseColumn(attribute: FloatArray?): String? {
        return attribute?.joinToString(",", "[", "]")
    }

    override fun convertToEntityAttribute(dbData: String?): FloatArray? {
        if (dbData.isNullOrBlank()) return null

        val trimmed = dbData.trim().removePrefix("[").removeSuffix("]")
        if (trimmed.isBlank()) return FloatArray(0)

        return trimmed.split(",")
            .map { it.trim().toFloat() }
            .toFloatArray()
    }
}