package com.otoki.powersales.domain.sales.entity.converter

import com.otoki.powersales.domain.sales.enums.SalesMonth
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class SalesMonthConverter : AttributeConverter<SalesMonth, String> {

    override fun convertToDatabaseColumn(attribute: SalesMonth?): String? =
        attribute?.value

    override fun convertToEntityAttribute(dbData: String?): SalesMonth? =
        SalesMonth.fromValueOrNull(dbData)
}
