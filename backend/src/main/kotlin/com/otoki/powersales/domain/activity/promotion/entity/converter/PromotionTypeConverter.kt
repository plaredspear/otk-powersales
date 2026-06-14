package com.otoki.powersales.domain.activity.promotion.entity.converter

import com.otoki.powersales.domain.activity.promotion.enums.PromotionType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class PromotionTypeConverter : AttributeConverter<PromotionType, String> {

    override fun convertToDatabaseColumn(attribute: PromotionType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): PromotionType? =
        PromotionType.fromDisplayNameOrNull(dbData)
}
