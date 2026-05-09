package com.otoki.powersales.claim.entity.converter

import com.otoki.powersales.claim.entity.ClaimChannel
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ClaimChannelConverter : AttributeConverter<ClaimChannel, String> {

    override fun convertToDatabaseColumn(attribute: ClaimChannel?): String? =
        attribute?.name

    override fun convertToEntityAttribute(dbData: String?): ClaimChannel? =
        ClaimChannel.fromCodeOrNull(dbData)
}
