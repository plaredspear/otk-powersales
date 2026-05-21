package com.otoki.powersales.auth.converter

import com.otoki.powersales.auth.entity.UserRoleEnum
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.slf4j.LoggerFactory

/**
 * Employee.role 컬럼용 JPA AttributeConverter.
 *
 * DB 에는 `UserRole.name` (영문 enum.name) 으로 저장한다.
 * Spec #573 P1-B — `app_authority` 한글 컬럼을 `role` 영문 enum.name 컬럼으로 마이그레이션
 * (V16) 한 정책에 맞춤. Read 시 enum.valueOf 실패하면 WARN 로그 후 [UserRoleEnum.UNKNOWN] fallback.
 */
@Converter(autoApply = false)
class UserRoleConverter : AttributeConverter<UserRoleEnum, String> {

    private val logger = LoggerFactory.getLogger(UserRoleConverter::class.java)

    override fun convertToDatabaseColumn(attribute: UserRoleEnum?): String? = attribute?.name

    override fun convertToEntityAttribute(dbData: String?): UserRoleEnum? {
        if (dbData.isNullOrEmpty()) return null
        return try {
            UserRoleEnum.valueOf(dbData)
        } catch (_: IllegalArgumentException) {
            logger.warn("Unknown UserRole DB value: {} — fallback to UNKNOWN", dbData)
            UserRoleEnum.UNKNOWN
        }
    }
}
