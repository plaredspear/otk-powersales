package com.otoki.powersales.domain.foundation.product.entity.converter

import com.otoki.powersales.domain.foundation.product.enums.StorageCondition
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class StorageConditionConverter : AttributeConverter<StorageCondition, String> {

    override fun convertToDatabaseColumn(attribute: StorageCondition?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): StorageCondition? =
        StorageCondition.Companion.fromDisplayNameOrNull(dbData)
}
