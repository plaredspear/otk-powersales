package com.otoki.powersales.sales.entity.converter

import com.otoki.powersales.sales.enums.SalesYear
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class SalesYearConverter : AttributeConverter<SalesYear, String> {

    override fun convertToDatabaseColumn(attribute: SalesYear?): String? =
        attribute?.value

    override fun convertToEntityAttribute(dbData: String?): SalesYear? =
        SalesYear.fromValueOrNull(dbData)
}
