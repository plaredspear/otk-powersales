package com.otoki.powersales.auth.converter

import com.otoki.powersales.auth.entity.UserRole
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.slf4j.LoggerFactory

/**
 * Employee.role 컬럼용 JPA AttributeConverter.
 *
 * DB 에는 `enum.name`(영문, 예: `LEADER`) 으로 저장한다.
 * Read 시 미정의 값(예: 마이그레이션 누락)은 [UserRole.UNKNOWN] 으로 fallback 한다.
 */
@Converter(autoApply = false)
class UserRoleConverter : AttributeConverter<UserRole, String> {

    private val logger = LoggerFactory.getLogger(UserRoleConverter::class.java)

    override fun convertToDatabaseColumn(attribute: UserRole?): String? = attribute?.name

    override fun convertToEntityAttribute(dbData: String?): UserRole? {
        if (dbData.isNullOrEmpty()) return null
        return try {
            UserRole.valueOf(dbData)
        } catch (_: IllegalArgumentException) {
            logger.warn("Unknown UserRole DB value: {} — falling back to UNKNOWN", dbData)
            UserRole.UNKNOWN
        }
    }
}
