package com.otoki.powersales.platform.common.entity.converter

import com.otoki.powersales.platform.common.enums.WorkingCategory3
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class WorkingCategory3Converter : AttributeConverter<WorkingCategory3, String> {

    override fun convertToDatabaseColumn(attribute: WorkingCategory3?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): WorkingCategory3? =
        WorkingCategory3.fromDisplayNameOrNull(dbData)
}
