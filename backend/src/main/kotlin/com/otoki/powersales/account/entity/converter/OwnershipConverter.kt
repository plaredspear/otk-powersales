package com.otoki.powersales.account.entity.converter

import com.otoki.powersales.account.entity.Ownership
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class OwnershipConverter : AttributeConverter<Ownership, String> {

    override fun convertToDatabaseColumn(attribute: Ownership?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): Ownership? =
        Ownership.fromDisplayNameOrNull(dbData)
}
