package com.otoki.powersales.common.entity.converter

import com.otoki.powersales.common.enums.EvaluationType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class EvaluationTypeConverter : AttributeConverter<EvaluationType, String> {

    override fun convertToDatabaseColumn(attribute: EvaluationType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): EvaluationType? =
        EvaluationType.fromDisplayNameOrNull(dbData)
}
