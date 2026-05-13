package com.otoki.powersales.product.entity.converter

import com.otoki.powersales.product.entity.Initiator
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class InitiatorConverter : AttributeConverter<Initiator, String> {

    override fun convertToDatabaseColumn(attribute: Initiator?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): Initiator? =
        Initiator.fromDisplayNameOrNull(dbData)
}
