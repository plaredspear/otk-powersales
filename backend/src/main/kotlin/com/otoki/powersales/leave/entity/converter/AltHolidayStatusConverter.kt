package com.otoki.powersales.leave.entity.converter

import com.otoki.powersales.leave.enums.AltHolidayStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class AltHolidayStatusConverter : AttributeConverter<AltHolidayStatus, String> {

    override fun convertToDatabaseColumn(attribute: AltHolidayStatus?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): AltHolidayStatus? =
        AltHolidayStatus.fromDisplayNameOrNull(dbData)
}
