package com.otoki.powersales.domain.support.notice.entity

import com.otoki.powersales.domain.support.notice.enums.NoticeScope
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class NoticeScopeConverter : AttributeConverter<NoticeScope?, String?> {

    override fun convertToDatabaseColumn(attribute: NoticeScope?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): NoticeScope? =
        NoticeScope.Companion.fromDisplayNameOrNull(dbData)
}
