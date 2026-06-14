package com.otoki.powersales.domain.activity.schedule.entity.converter

import com.otoki.powersales.domain.activity.schedule.enums.SecondWorkType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class SecondWorkTypeConverter : AttributeConverter<SecondWorkType, String> {

    override fun convertToDatabaseColumn(attribute: SecondWorkType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): SecondWorkType? =
        SecondWorkType.fromDisplayNameOrNull(dbData)
}
