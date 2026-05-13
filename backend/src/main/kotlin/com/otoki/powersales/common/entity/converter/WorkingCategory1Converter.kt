package com.otoki.powersales.common.entity.converter

import com.otoki.powersales.common.entity.WorkingCategory1
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class WorkingCategory1Converter : AttributeConverter<WorkingCategory1, String> {

    override fun convertToDatabaseColumn(attribute: WorkingCategory1?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): WorkingCategory1? =
        WorkingCategory1.fromDisplayNameOrNull(dbData)
}
