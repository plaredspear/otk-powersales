package com.otoki.powersales.domain.foundation.account.entity.converter

import com.otoki.powersales.domain.foundation.account.entity.Industry
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class IndustryConverter : AttributeConverter<Industry, String> {

    override fun convertToDatabaseColumn(attribute: Industry?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): Industry? =
        Industry.Companion.fromDisplayNameOrNull(dbData)
}
