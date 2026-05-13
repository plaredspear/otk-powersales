package com.otoki.powersales.claim.entity.converter

import com.otoki.powersales.claim.entity.ClaimStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ClaimStatusConverter : AttributeConverter<ClaimStatus, String> {

    override fun convertToDatabaseColumn(attribute: ClaimStatus?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ClaimStatus? =
        ClaimStatus.fromDisplayNameOrNull(dbData)
}
