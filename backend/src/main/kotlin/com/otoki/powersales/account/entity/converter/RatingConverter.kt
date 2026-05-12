package com.otoki.powersales.account.entity.converter

import com.otoki.powersales.account.entity.Rating
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class RatingConverter : AttributeConverter<Rating, String> {

    override fun convertToDatabaseColumn(attribute: Rating?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): Rating? =
        Rating.fromDisplayNameOrNull(dbData)
}
