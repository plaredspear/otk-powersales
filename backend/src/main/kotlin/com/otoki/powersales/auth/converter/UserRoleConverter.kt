package com.otoki.powersales.auth.converter

import com.otoki.powersales.auth.entity.UserRole
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.slf4j.LoggerFactory

/**
 * Employee.role 컬럼용 JPA AttributeConverter.
 *
 * DB 에는 SF picklist 원본 옵션값 (`UserRole.korean`, 예: `조장` / `AccountViewAll`) 으로 저장한다.
 * SF Object 정합 정책 (`sf-object-meta/sandbox/README.md` §6.6 v2.2) — picklist DB 저장은 SF 원본 보존.
 * Read 시 미정의 값은 [UserRole.UNKNOWN] 으로 fallback 한다.
 */
@Converter(autoApply = false)
class UserRoleConverter : AttributeConverter<UserRole, String> {

    private val logger = LoggerFactory.getLogger(UserRoleConverter::class.java)

    override fun convertToDatabaseColumn(attribute: UserRole?): String? = attribute?.korean

    override fun convertToEntityAttribute(dbData: String?): UserRole? {
        if (dbData.isNullOrEmpty()) return null
        return UserRole.fromKorean(dbData)
    }
}
