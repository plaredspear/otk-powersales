package com.otoki.powersales.platform.common.entity.converter

import com.otoki.powersales.platform.common.enums.WorkingCategory5
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class WorkingCategory5Converter : AttributeConverter<WorkingCategory5, String> {

    override fun convertToDatabaseColumn(attribute: WorkingCategory5?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): WorkingCategory5? =
        WorkingCategory5.fromDisplayNameOrNull(dbData)
}
