package com.otoki.powersales.account.entity.converter

import com.otoki.powersales.account.entity.FreezerType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class FreezerTypeConverter : AttributeConverter<FreezerType, String> {

    override fun convertToDatabaseColumn(attribute: FreezerType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): FreezerType? =
        FreezerType.fromDisplayNameOrNull(dbData)
}
