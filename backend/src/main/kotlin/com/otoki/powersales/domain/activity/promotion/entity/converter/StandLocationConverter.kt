package com.otoki.powersales.domain.activity.promotion.entity.converter

import com.otoki.powersales.domain.activity.promotion.enums.StandLocation
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class StandLocationConverter : AttributeConverter<StandLocation, String> {

    override fun convertToDatabaseColumn(attribute: StandLocation?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): StandLocation? =
        StandLocation.fromDisplayNameOrNull(dbData)
}
