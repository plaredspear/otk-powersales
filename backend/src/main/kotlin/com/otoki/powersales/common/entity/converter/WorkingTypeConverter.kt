package com.otoki.powersales.common.entity.converter

import com.otoki.powersales.common.enums.WorkingType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class WorkingTypeConverter : AttributeConverter<WorkingType, String> {

    override fun convertToDatabaseColumn(attribute: WorkingType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): WorkingType? =
        WorkingType.fromDisplayNameOrNull(dbData)
}
