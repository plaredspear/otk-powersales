package com.otoki.powersales.product.entity.converter

import com.otoki.powersales.product.enums.ManagementType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ManagementTypeConverter : AttributeConverter<ManagementType, String> {

    override fun convertToDatabaseColumn(attribute: ManagementType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ManagementType? =
        ManagementType.fromDisplayNameOrNull(dbData)
}
