package com.otoki.powersales.domain.activity.schedule.entity.converter

import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork3
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class TypeOfWork3Converter : AttributeConverter<TypeOfWork3, String> {

    override fun convertToDatabaseColumn(attribute: TypeOfWork3?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): TypeOfWork3? =
        TypeOfWork3.fromDisplayNameOrNull(dbData)
}
