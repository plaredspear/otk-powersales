package com.otoki.powersales.domain.activity.schedule.entity.converter

import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class TypeOfWork5Converter : AttributeConverter<TypeOfWork5, String> {

    override fun convertToDatabaseColumn(attribute: TypeOfWork5?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): TypeOfWork5? =
        TypeOfWork5.fromDisplayNameOrNull(dbData)
}
