package com.otoki.powersales.domain.foundation.product.entity.converter

import com.otoki.powersales.domain.foundation.product.enums.NewProductStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class NewProductStatusConverter : AttributeConverter<NewProductStatus, String> {

    override fun convertToDatabaseColumn(attribute: NewProductStatus?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): NewProductStatus? =
        NewProductStatus.Companion.fromDisplayNameOrNull(dbData)
}
