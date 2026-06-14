package com.otoki.powersales.domain.activity.promotion.entity.converter

import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ProfessionalPromotionTeamTypeConverter : AttributeConverter<ProfessionalPromotionTeamType, String> {

    override fun convertToDatabaseColumn(attribute: ProfessionalPromotionTeamType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ProfessionalPromotionTeamType? =
        ProfessionalPromotionTeamType.fromDisplayNameOrNull(dbData)
}
