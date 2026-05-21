package com.otoki.powersales.suggestion.entity.converter

import com.otoki.powersales.suggestion.entity.SuggestionActionStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class SuggestionActionStatusConverter : AttributeConverter<SuggestionActionStatus, String> {

    override fun convertToDatabaseColumn(attribute: SuggestionActionStatus?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): SuggestionActionStatus? =
        SuggestionActionStatus.fromDisplayNameOrNull(dbData)
}
