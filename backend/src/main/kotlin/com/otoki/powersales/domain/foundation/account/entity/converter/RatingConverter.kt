package com.otoki.powersales.domain.foundation.account.entity.converter

import com.otoki.powersales.domain.foundation.account.entity.Rating
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class RatingConverter : AttributeConverter<Rating, String> {

    override fun convertToDatabaseColumn(attribute: Rating?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): Rating? =
        Rating.Companion.fromDisplayNameOrNull(dbData)
}
