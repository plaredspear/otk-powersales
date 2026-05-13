package com.otoki.powersales.notice.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class NoticeScopeConverter : AttributeConverter<NoticeScope?, String?> {

    override fun convertToDatabaseColumn(attribute: NoticeScope?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): NoticeScope? =
        NoticeScope.fromDisplayNameOrNull(dbData)
}
