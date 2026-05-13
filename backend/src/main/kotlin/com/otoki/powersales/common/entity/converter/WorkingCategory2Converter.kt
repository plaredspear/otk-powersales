package com.otoki.powersales.common.entity.converter

import com.otoki.powersales.common.entity.WorkingCategory2
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class WorkingCategory2Converter : AttributeConverter<WorkingCategory2, String> {

    override fun convertToDatabaseColumn(attribute: WorkingCategory2?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): WorkingCategory2? =
        WorkingCategory2.fromDisplayNameOrNull(dbData)
}
