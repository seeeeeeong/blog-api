package com.blog.api.common.jpa

import com.pgvector.PGvector
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Hibernate UserType for pgvector
 * FloatArray <-> PostgreSQL vector 타입 변환
 */
class VectorType : UserType<FloatArray> {

    override fun getSqlType(): Int = Types.OTHER

    override fun returnedClass(): Class<FloatArray> = FloatArray::class.java

    override fun equals(x: FloatArray?, y: FloatArray?): Boolean {
        return x.contentEquals(y)
    }

    override fun hashCode(x: FloatArray?): Int {
        return x.contentHashCode()
    }

    override fun nullSafeGet(
        rs: ResultSet,
        position: Int,
        session: SharedSessionContractImplementor?,
        owner: Any?
    ): FloatArray? {
        val obj = rs.getObject(position) ?: return null
        return if (obj is PGvector) {
            obj.toArray()
        } else null
    }

    override fun nullSafeSet(
        st: PreparedStatement,
        value: FloatArray?,
        index: Int,
        session: SharedSessionContractImplementor?
    ) {
        if (value == null) {
            st.setNull(index, Types.OTHER)
        } else {
            st.setObject(index, PGvector(value))
        }
    }

    override fun deepCopy(value: FloatArray?): FloatArray? {
        return value?.copyOf()
    }

    override fun isMutable(): Boolean = true

    override fun disassemble(value: FloatArray?): Serializable? {
        return deepCopy(value)
    }

    override fun assemble(cached: Serializable?, owner: Any?): FloatArray? {
        return deepCopy(cached as? FloatArray)
    }
}
