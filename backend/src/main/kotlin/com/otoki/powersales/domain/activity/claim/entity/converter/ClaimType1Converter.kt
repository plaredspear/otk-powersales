package com.otoki.powersales.domain.activity.claim.entity.converter

import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ClaimType1Converter : AttributeConverter<ClaimType1, String> {

    override fun convertToDatabaseColumn(attribute: ClaimType1?): String? =
        attribute?.value

    override fun convertToEntityAttribute(dbData: String?): ClaimType1? =
        ClaimType1.fromValueOrNull(dbData)
}
