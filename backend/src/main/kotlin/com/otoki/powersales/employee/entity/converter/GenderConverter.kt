package com.otoki.powersales.employee.entity.converter

import com.otoki.powersales.employee.entity.Gender
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.slf4j.LoggerFactory

/**
 * Employee.gender 컬럼용 JPA AttributeConverter.
 *
 * DB 에는 SF picklist 원본값(예: `남` / `여`)으로 저장한다.
 * Read 시 구 enum name 값(`MALE` / `FEMALE`)도 backward-compat으로 허용 — 별도 데이터 마이그레이션 불필요.
 */
@Converter(autoApply = false)
class GenderConverter : AttributeConverter<Gender?, String?> {

    private val logger = LoggerFactory.getLogger(GenderConverter::class.java)

    override fun convertToDatabaseColumn(attribute: Gender?): String? = attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): Gender? {
        if (dbData.isNullOrEmpty()) return null
        return when (dbData) {
            "남" -> Gender.MALE
            "여" -> Gender.FEMALE
            "MALE" -> {
                logger.debug("Legacy gender value 'MALE' — reading as Gender.MALE")
                Gender.MALE
            }
            "FEMALE" -> {
                logger.debug("Legacy gender value 'FEMALE' — reading as Gender.FEMALE")
                Gender.FEMALE
            }
            else -> {
                logger.warn("Unknown gender DB value: {} — returning null", dbData)
                null
            }
        }
    }
}
