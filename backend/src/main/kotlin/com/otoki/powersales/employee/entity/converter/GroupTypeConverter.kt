package com.otoki.powersales.employee.entity.converter

import com.otoki.powersales.employee.enums.GroupType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.slf4j.LoggerFactory

/**
 * `Group.type` 컬럼용 JPA AttributeConverter (Spec #755 Q6).
 *
 * DB 에는 SF picklist 원본값(예: `Regular` / `Queue` / `Role`)으로 저장.
 * 미지의 옵션값 수신 시 null 반환 + 경고 로그 (§6.6 v2.2 정책).
 */
@Converter(autoApply = false)
class GroupTypeConverter : AttributeConverter<GroupType?, String?> {

    private val logger = LoggerFactory.getLogger(GroupTypeConverter::class.java)

    override fun convertToDatabaseColumn(attribute: GroupType?): String? = attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): GroupType? {
        if (dbData.isNullOrBlank()) return null
        val matched = GroupType.fromDisplayNameOrNull(dbData)
        if (matched == null) {
            logger.warn("Unknown GroupType DB value: {} — returning null", dbData)
        }
        return matched
    }
}
