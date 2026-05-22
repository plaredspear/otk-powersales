package com.otoki.powersales.suggestion.entity.converter

import com.otoki.powersales.suggestion.entity.SuggestionCategory
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class SuggestionCategoryConverter : AttributeConverter<SuggestionCategory, String> {

    override fun convertToDatabaseColumn(attribute: SuggestionCategory?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): SuggestionCategory? =
        SuggestionCategory.fromDisplayNameOrNull(dbData)
}
