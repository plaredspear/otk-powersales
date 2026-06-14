package com.otoki.powersales.domain.activity.schedule.entity.converter

import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class TypeOfWork1Converter : AttributeConverter<TypeOfWork1, String> {

    override fun convertToDatabaseColumn(attribute: TypeOfWork1?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): TypeOfWork1? =
        TypeOfWork1.fromDisplayNameOrNull(dbData)
}
