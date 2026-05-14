package com.otoki.powersales.employee.entity.converter

import com.otoki.powersales.employee.enums.CrmWorkType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class CrmWorkTypeConverter : AttributeConverter<CrmWorkType?, String?> {

    override fun convertToDatabaseColumn(attribute: CrmWorkType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): CrmWorkType? =
        CrmWorkType.fromDisplayNameOrNull(dbData)
}
