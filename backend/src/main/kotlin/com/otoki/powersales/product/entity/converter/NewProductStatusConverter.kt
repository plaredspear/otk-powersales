package com.otoki.powersales.product.entity.converter

import com.otoki.powersales.product.entity.NewProductStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class NewProductStatusConverter : AttributeConverter<NewProductStatus, String> {

    override fun convertToDatabaseColumn(attribute: NewProductStatus?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): NewProductStatus? =
        NewProductStatus.fromDisplayNameOrNull(dbData)
}
