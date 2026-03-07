package com.otoki.internal.notice.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class NoticeCategoryConverter : AttributeConverter<NoticeCategory?, String?> {

    override fun convertToDatabaseColumn(attribute: NoticeCategory?): String? {
        return attribute?.dbValue
    }

    override fun convertToEntityAttribute(dbData: String?): NoticeCategory? {
        if (dbData.isNullOrBlank()) return null
        return NoticeCategory.fromDbValue(dbData)
    }
}
