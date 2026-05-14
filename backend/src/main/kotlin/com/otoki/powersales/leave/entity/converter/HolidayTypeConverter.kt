package com.otoki.powersales.leave.entity.converter

import com.otoki.powersales.leave.enums.HolidayType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class HolidayTypeConverter : AttributeConverter<HolidayType, String> {

    override fun convertToDatabaseColumn(attribute: HolidayType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): HolidayType? =
        HolidayType.fromDisplayNameOrNull(dbData)
}
