package com.otoki.powersales.domain.activity.claim.entity.converter

import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.RequestType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * SF multipicklist `DKRetail__RequestType__c` AttributeConverter.
 * DB ↔ Set<enum> 양방향. DB 저장 포맷: ";"-구분 CSV (e.g., "의견서;상담;긴급처리(FS사업부)").
 * SF Heroku Connect 가 multipicklist 를 ";" 구분으로 직렬화하는 컨벤션과 정합.
 */
@Converter
class RequestTypeSetConverter : AttributeConverter<Set<RequestType>, String> {

    companion object {
        private const val SEPARATOR = ";"
    }

    override fun convertToDatabaseColumn(attribute: Set<RequestType>?): String? {
        if (attribute.isNullOrEmpty()) return null
        return attribute.joinToString(SEPARATOR) { it.displayName }
    }

    override fun convertToEntityAttribute(dbData: String?): Set<RequestType> {
        if (dbData.isNullOrBlank()) return emptySet()
        return dbData.split(SEPARATOR)
            .mapNotNull { RequestType.fromDisplayNameOrNull(it.trim()) }
            .toSet()
    }
}
