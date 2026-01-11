package com.blog.api.storage.converter

import com.pgvector.PGvector
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class PgVectorConverter : AttributeConverter<FloatArray?, PGvector?> {

    override fun convertToDatabaseColumn(attribute: FloatArray?): PGvector? {
        if (attribute == null) return null
        return PGvector(attribute)
    }

    override fun convertToEntityAttribute(dbData: PGvector?): FloatArray? {
        return dbData?.toArray()
    }
}
