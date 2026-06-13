package com.otoki.powersales.domain.activity.claim.entity.converter

import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ClaimType2Converter : AttributeConverter<ClaimType2, String> {

    override fun convertToDatabaseColumn(attribute: ClaimType2?): String? =
        attribute?.value

    override fun convertToEntityAttribute(dbData: String?): ClaimType2? =
        ClaimType2.fromValueOrNull(dbData)
}
